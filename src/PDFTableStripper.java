// Created by Eshita Shukla

import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.text.TextPosition;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.List;
import java.util.*;


// THIS IS THE MAIN CLASS

public class PDFTableStripper extends PDFTextStripper
{

    /**
     * @param args The command line arguments.
     *
     * @throws IOException If there is an error parsing the document.
     */
    public static void main(String[] args) throws IOException
    {
        // Function to get all data from the pdf
        Details_new[] D0 = getDetails("/home/theperson/Downloads/sample.pdf", "/home/theperson/cofigFile.txt");// "/home/theperson/Downloads/PayPal_ Transaction Details_Order# - TX76017634.pdf");

    }

    public static Details_new[] getDetails(String fileName, String configFileName) throws IOException {

        // Some helper variables are declared
        double[] rowCoordinates;
        double[] rowHeights;
        int[] rowPage;

        // PDF units are at 72 DPI
        // This number changes with the quality of the pdf
        final double res = 72;

        PDFTableStripper stripper = new PDFTableStripper();
        stripper.setSortByPosition(true);

        try (PDDocument document = PDDocument.load(new File(fileName)))
        {
            // ****************** PART 1 ******************************************************
            // Extract TEXT data from each page on the pdf


            // 9x9 inch area is considered on each page
            // Overflow throws no error
            stripper.setRegion(new Rectangle((int) Math.round(0.0*res), (int) Math.round(1*res), (int) Math.round(9*res), (int) Math.round(9.0*res)));

            // Calculating the total number of rows (all pages)
            int totalNoOfRows = 0;
            for (int page = 0; page < document.getNumberOfPages(); ++page) {
                // Initialising page
                PDPage pdPage = document.getPage(page);
                // Extracting table (not accurate) - only done for the purpose of conuting rows
                Rectangle2D[][] regions = stripper.extractTable(pdPage);
                int R = stripper.getRows();
                totalNoOfRows +=R;
            }

            // Arrays to keep track of Y coordinates, heights, and page numbers
            rowCoordinates = new double[totalNoOfRows];
            rowHeights = new double[totalNoOfRows];
            rowPage = new int[totalNoOfRows];

            // Extract data from each page on the pdf
            int R = 0;
            for (int page = 0; page < document.getNumberOfPages(); ++page) {

                // Initialising page
                PDPage pdPage = document.getPage(page);
                // Extracting table (not accurate) - only done for the purpose of counting rows
                Rectangle2D[][] regions = stripper.extractTable(pdPage);

                // Calculating the dimentions and geometrical positions of all the rows on each page
                for(int r=0; r<stripper.getRows(); ++r) {
                    for(int c=0; c<stripper.getColumns(); ++c) {
                        Rectangle2D region = regions[c][r];
                        rowCoordinates[R] = region.getMinY();
                        rowHeights[R] = region.getHeight();
                        rowPage[R] = page;
                    }
                    R +=1;
                }
            }

            // Stripping the entire document into rows
            int r = 0;
            double[][][] rowCooordHeight = new double[rowCoordinates.length][15][2];
            String[][] rowColumnWiseContent = new String[rowCoordinates.length][15];
            for (int i = 0; i< rowCoordinates.length; i++){

                stripper = new PDFTableStripper();
                stripper.setSortByPosition(true);

                // Isolating each row
                stripper.setRegion(new Rectangle((int) Math.round(0.0*res), (int) Math.round(rowCoordinates[i]), (int) Math.round(9*res), (int) Math.round(rowHeights[i])));

                // Repeat for each page of the PDF
                int page = rowPage[i];
                PDPage pdPage = document.getPage(page);
                Rectangle2D[][] regions = stripper.extractTable(pdPage);

                // Iterating through all columns of the row (only one row in each area)
                for(int c=0; c<stripper.getColumns(); ++c) {
                    Rectangle2D region = regions[c][r];
                    rowCoordinates[r] = region.getMinY();
                    String text = stripper.getText(r, c);
                    rowColumnWiseContent[i][c] = text;
                    rowCooordHeight[i][c][0] = region.getMinX();
                    rowCooordHeight[i][c][1] = region.getMaxX();
                }
            }



            // ****************** PART 2 ******************************************************
            // Using row coordinates calculated above, divide the pdf into rectangles
            // Extract all contents from each rectangle , and partition the content into columns
            // i is row number; c is column number

            // Calculating the actual highest number of columns of all the rows
            int highestActualNumOfCol = 0;
            for (int i = 0; i<rowCoordinates.length; i++){
                int actualNumOfCol = 0;
                for (int c = 0; c<10; c++){
                    if (rowColumnWiseContent[i][c] != null){
                        actualNumOfCol = actualNumOfCol +1;
                    }
                }
                if (actualNumOfCol>highestActualNumOfCol){
                    highestActualNumOfCol = actualNumOfCol;
                }
            }



            // ****************** PART 3 ******************************************************
            // This part is only for the new algorithm
            // Calculating the partitions of all rows

            // Throughout a column, X is same
            // Array to save the coordinate where each columnis separated from the next
            double[][] rowPartitions = new double[rowCoordinates.length][highestActualNumOfCol+1];
            double[][] rowPartitions_2 = new double[rowCoordinates.length][highestActualNumOfCol+1];
            for (int i = 0; i<rowCoordinates.length; i++){
                for (int j=0; j<highestActualNumOfCol; j++){

                    double x1 = rowCooordHeight[i][j][0];
                    double x2 = rowCooordHeight[i][j][1];
                    boolean found1 = false;
                    boolean found2 = false;

                    int firstNullElem = 0;
                    for (int k=0; k<highestActualNumOfCol;k++){
                        if (rowPartitions[i][k] == 0.0){
                            firstNullElem = k;
                            break;
                        }
                        if (rowPartitions[i][k] == x1){
                            found1 = true;
                        }
                        if (rowPartitions[i][k] == x2){
                            found2 = true;
                        }
                        if (found1 && found2){
                            break;
                        }
                    }
                    if (!found1 && x1!=0.0){
                        rowPartitions[i][firstNullElem] = x1;
                        firstNullElem = firstNullElem + 1;
                    }
                }
            }

            for (int i = 0; i<rowCoordinates.length; i++){
                for (int j=0; j<highestActualNumOfCol; j++){

                    double x1 = rowCooordHeight[i][j][0];
                    double x2 = rowCooordHeight[i][j][1];
                    boolean found1 = false;
                    boolean found2 = false;

                    int firstNullElem = 0;
                    for (int k=0; k<highestActualNumOfCol;k++){
                        if (rowPartitions_2[i][k] == 0.0){
                            firstNullElem = k;
                            break;
                        }
                        if (rowPartitions_2[i][k] == x1){
                            found1 = true;
                        }
                        if (rowPartitions_2[i][k] == x2){
                            found2 = true;
                        }
                        if (found1 && found2){
                            break;
                        }
                    }

                    if (!found2 && x2!=0.0){
                        rowPartitions_2[i][firstNullElem] = x2;
                    }
                }
            }

//          This commented code is for debugging purposes only

//            System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
//            for (int i = 0; i<rowCoordinates.length; i++) {
//                System.out.println("Row: " + i);
//                for (int j = 0; j < highestActualNumOfCol; j++) {
//                    System.out.println(rowPartitions[i][j]);
//                }
//            }

//            // This commented snippet is only for debugging purposes
//
//            int[][] table0= new int[headingFoundStartPointer][rowCoordinates.length];
//            for (int i = 0; i<headingFoundStartPointer; i++){
//                for (int j =rowsWithHeadingsStart[i]; j<=rowsWithHeadingsEnd[i]; j++){
//                    String S = "";
//                    for (int k=0; k<highestActualNumOfCol; k++){
//                        if (rowColumnWiseContent[j][k] !=null){
//                            S = S+rowColumnWiseContent[j][k];
//                        }
//                    }
//                    table0[i][j-rowsWithHeadingsStart[i]] = j;
//                }
//            }
//            System.out.println("%%%%%%%%%%%%%%%%%%%%%%");

            int b;

            // ****************** PART 4 ******************************************************
            // Finding the "Heading" rows (only start), using "points-based-system"
            // Each row's contents are examined
            // Each row is given point, then the points are used to decide if a row is a "Heading" or not


            int[] headingPointsForEachRow = new int[rowCoordinates.length];
            boolean[] rowWithHeadings = new boolean[rowCoordinates.length];

            // Variable keeps track of the number of heading found
            int numOfHeadingRows = 0;
            for (int i = 0; i<rowCoordinates.length; i++){

                for(int j = 0; j<highestActualNumOfCol; j++){
                    String Content = rowColumnWiseContent[i][j];
                    // If certain keywords are found in the content of any of the columns, points are increased by 1 for each keyword
                    if (Content!=null) {

                        try {
                            File file=new File(configFileName);    //creates a new file instance
                            FileReader fr=new FileReader(file);   //reads the file
                            BufferedReader br=new BufferedReader(fr);  //creates a buffering character input stream
                            StringBuffer sb=new StringBuffer();    //constructs a string buffer with no characters
                            String line;
                            while((line=br.readLine())!=null)
                            {
//                                sb.append(line);      //appends line to string buffer
//                                sb.append("\n");     //line feed
                                if (Content.contains(line)){
                                    headingPointsForEachRow[i]+=1;
                                }
                            }
                            fr.close();    //closes the stream and release the resources
//                            System.out.println("Contents of File: ");
//                            System.out.println(sb.toString());   //returns a string that textually represents the object
                        }
                        catch(IOException e) {
                            e.printStackTrace();
                        }


                        if (Content.contains("Sr.") || Content.contains("Sl.")) {
                            headingPointsForEachRow[i] += 1;
                        }
//                        if (Content.contains("Description") || Content.contains("DESCRIPTION") || Content.contains("description")) {
//                            headingPointsForEachRow[i] += 1;
//                        }
//                        if (Content.contains("Amount") || Content.contains("AMOUNT") || Content.contains("amount")) {
//                            headingPointsForEachRow[i] += 1;
//                        }
//                        if (Content.contains("Withdrawal")) {
//                            headingPointsForEachRow[i] += 1;
//                        }
//                        if (Content.contains("Deposit")) {
//                            headingPointsForEachRow[i] += 1;
//                        }
//                        if (Content.contains(" No.")) {
//                            headingPointsForEachRow[i] += 1;
//                        }
//                        // Special symbols like the "rupees-symbol" when converted to string look like --> "`"
//                        if (Content.contains("`")) {
//                            headingPointsForEachRow[i] += 1;
////                            System.out.println("^^^^^^^^^^^" + Content);
//                        }

                    }
                }

                // If a row has certain amount of points, the row is considered a heading
                if (headingPointsForEachRow[i]>=2){
//                    System.out.println("Heading found!" + rowColumnWiseContent[i][0]);
                    rowWithHeadings[i] = true;
                    numOfHeadingRows ++;
                }
                else{
                    rowWithHeadings[i] = false;
                }
            }

            // ****************** PART 5 ******************************************************
            // Now that we know which rows are headings and which are not, we create tables
            // Rows occuring consecutively, after the heading (and have similar partitions) will be grouped together
            // "Similar Partitions" means that the partitions of columns of the rows have 15 dpi different between the two

            Details_new[] arrayOfDetails = new Details_new[numOfHeadingRows];
            int currentHeading;
            boolean tableHasBegun = false;
            int i = 0;
            List<int[]> allTables = new ArrayList<int[]>();
            int detailsPointer = 0;

            // Iterating through all row (Headings only, in the outer loop)
            while (i<rowCoordinates.length){ //
                if (rowWithHeadings[i] == true){

                    arrayOfDetails[detailsPointer] = new Details_new();
                    arrayOfDetails[detailsPointer].setPageNo(rowPage[i]);

//                    System.out.println("*** ");
                    tableHasBegun = true;
                    currentHeading = i;


                    r=0;
                    int j = i+1;
//                    System.out.println(j);

                    // Used for table coordinates in the "Details" object
                    double minY = rowPartitions[i][0];
                    double maxY = rowPartitions_2[i][0];

                    String[][] Tables_temp = new String[totalNoOfRows][highestActualNumOfCol];

                    // Coordinates of the first row
                    for (int l=0; l<highestActualNumOfCol; l++){
                        double x_1 = rowPartitions[i][l];
                        double x_2 = rowPartitions_2[i][l];
                        if (x_1<minY){
                            minY = x_1;
                        }
                        if (x_2>maxY){
                            maxY = x_2;
                        }
                        Tables_temp[r][l] = rowColumnWiseContent[i][l];
                    }

                    // Iterating through the consecutive rows, of the heading row
                    while(j<rowCoordinates.length){
                        if (rowWithHeadings[j]==true){
                            break;
                        }
                        int pointsForSimilarityWithHeading = 0;
                        int numberOfColumnsInCurrentRow = 0;

                        // Iterate through all columns of the current row
                        for (int m=0; m<highestActualNumOfCol; m++){
                            if (rowColumnWiseContent[j][m]!=null){
                                numberOfColumnsInCurrentRow++;
                            }

//                            System.out.println("content: " + rowColumnWiseContent[j][m]);
                            double x_1 = rowPartitions[j][m];
                            double x_2 = rowPartitions_2[j][m];
                            if (x_1<minY){
                                minY = x_1;
                            }
                            if (x_2>maxY){
                                maxY = x_2;
                            }
                            // Iterate thorugh all columns of the heading row
                            for (int n=0; n<highestActualNumOfCol; n++){

                                double diff = rowPartitions[currentHeading][m]- rowPartitions[j][n];
                                if (diff>-15 && diff<15){
//                                    System.out.println(pointsForSimilarityWithHeading + " " + j);
                                    pointsForSimilarityWithHeading++;
                                    tableHasBegun = false;
                                    break;
                                }
                                double diff_2 = rowPartitions_2[currentHeading][m]- rowPartitions_2[j][n];
                                if (diff_2>-15 && diff_2<15){
//                                    System.out.println(pointsForSimilarityWithHeading + " " + j);
                                    pointsForSimilarityWithHeading++;
                                    tableHasBegun = false;
                                    break;
                                }
                                double diff_3 = ((rowPartitions_2[currentHeading][m]-rowPartitions[currentHeading][m])/2 +rowPartitions[currentHeading][m])- ((rowPartitions_2[currentHeading][m]-rowPartitions[j][n])/2 +rowPartitions[j][n]);
                                if (diff_3>-15 && diff_3<15){
//                                    System.out.println(pointsForSimilarityWithHeading + " " + j);
                                    pointsForSimilarityWithHeading++;
                                    tableHasBegun = false;
                                    break;
                                }
                            }
                        }
//                        System.out.println("Points: " + pointsForSimilarityWithHeading + " Columns: " + numberOfColumnsInCurrentRow);

                        // If the row is similar to the heading row
                        if (pointsForSimilarityWithHeading>=numberOfColumnsInCurrentRow-1 && numberOfColumnsInCurrentRow>=2){
//                            System.out.println("Same Table");
                            int[] nearestColumnForLeftAligned = new int[highestActualNumOfCol];
                            String[] contents = new String[highestActualNumOfCol];
                            for (int p=0; p<highestActualNumOfCol; p++){
                                int nearestIndex = 0;
                                double minDiff = rowCooordHeight[j][p][0]-rowCooordHeight[i][nearestIndex][1];
                                for (int q=0; q<highestActualNumOfCol;q++){
                                    int currentIndex = q;
                                    double diff = rowCooordHeight[j][p][0]-rowCooordHeight[i][currentIndex][1];
//                                    System.out.println("@@@" + currentIndex + " " +nearestIndex + " " + rowColumnWiseContent[i][p]);
//                                    System.out.println(diff + " " + minDiff + " " + rowColumnWiseContent[i][p].charAt(0) + rowColumnWiseContent[i][p].charAt(1) + " " + rowColumnWiseContent[j][p]);



                                    if (minDiff<0){
                                        break;
                                    }
//                                    if (diff<-50 ){
////                                        if (p != 0){
////                                            if (nearestIndex!=nearestColumnForLeftAligned[p-1]){
////                                                break;
////                                            }
////                                        }
////                                        else{
////                                            break;
////                                        }
//                                        break;
//                                    }
                                    else if (diff<minDiff){
                                        nearestIndex=currentIndex;
                                        nearestColumnForLeftAligned[p] = nearestIndex;
                                        minDiff = diff;
                                    }
                                }

                                try {
                                    if (rowColumnWiseContent[j][p].contains("TOTAL:")) {
//                                        System.out.println(rowColumnWiseContent[j][p] + " 666666666666666666666666 " + (r+1) + " " + nearestColumnForLeftAligned[p]);
                                    }
                                }
                                catch (Exception e){}

//                                System.out.println(p + "^^^^^^^^^" + nearestColumnForLeftAligned[p] + "^^^^^^^^^");//+ rowColumnWiseContent[j][p] + "______________________\n");

                                if (Tables_temp[r+1][nearestColumnForLeftAligned[p]]==null){
                                    Tables_temp[r+1][nearestColumnForLeftAligned[p]] = rowColumnWiseContent[j][p];

                                }
//                                System.out.println(Tables_temp[r][0] + "&&&&&&&&&&&");
                            }

                            j++;
                            r++;
                        }
                        // If the row is not similar to the heading row
                        else{
//                            System.out.println("BREAKKKKKKKKKKKKKKKKKKKKKKKKKKKKKk");
                            break;
                        }
                    }

                    // Creating the Tables 2D array of strings, for the "Details" object
                    String[][] Tables = new String[r+1][highestActualNumOfCol];
                    for (int m=0; m<=r; m++){
//                        System.out.println("__________________");
                        for (int n=0; n<highestActualNumOfCol; n++){
//                            System.out.println(m + " " +n);
//                            String Content = rowColumnWiseContent[i+m][n];
//                            Tables[m][n] = Content;

//                            System.out.println("******" +Tables_temp[m][n] + " " + Tables_temp[1][0]);
                        }
                    }

                    // Adding table to the "Details" object
                    arrayOfDetails[detailsPointer].setTables(Tables_temp);

                    // Adding coordinates to the "Details" object
                    double x1,x2,y1,y2;
                    x1 = rowCoordinates[i];
                    x2 = rowCoordinates[i+r]+rowHeights[i+r];
                    y1 = minY;
                    y2 = maxY;
                    arrayOfDetails[detailsPointer].setTableAllPoints(new double[]{x1, x2, y1, y2});

                    stripper.setRegion(new Rectangle((int) Math.round(0.0*res), (int) Math.round(x1), (int) Math.round(9*res), (int) Math.round((x2-x1))));

//                    // Calculating the total number of rows (all pages)
////                    int totalNoOfRows = 0;
////                    for (int page = 0; page < document.getNumberOfPages(); ++page) {
////                        // Initialising page
////                        PDPage pdPage = document.getPage(page);
////                        // Extracting table (not accurate) - only done for the purpose of conuting rows
////                        Rectangle2D[][] regions = stripper.extractTable(pdPage);
////                        int R = stripper.getRows();
////                        totalNoOfRows +=R;
////                    }
//
//                    // Arrays to keep track of Y coordinates, heights, and page numbers
//                    rowCoordinates = new double[totalNoOfRows];
//                    rowHeights = new double[totalNoOfRows];
//                    rowPage = new int[totalNoOfRows];

                    // Extract data from each page on the pdf
//                    int R = 0;
//                    for (int page = 0; page < document.getNumberOfPages(); ++page) {
/*
                    // Initialising page
                    int page = arrayOfDetails[detailsPointer].getPageNo();
                    PDPage pdPage = document.getPage(page);
                    Rectangle2D[][] regions = stripper.extractTable(pdPage);
                    String[][] TablesFinal = new String[stripper.getRows()][stripper.getColumns()];
                    System.out.println("NNNNNNNNNNNNN "+stripper.getRows() + " " + stripper.getColumns());
                    // Calculating the dimentions and geometrical positions of all the rows on each page
                    for(r=0; r<stripper.getRows(); ++r) {
                        for(int c=0; c<stripper.getColumns(); ++c) {
                            Rectangle2D region = regions[c][r];
                        }
                    }
*/


                    detailsPointer++;
                    i = j;
                    tableHasBegun = false;
                }
                else if(tableHasBegun == false){
                    i++;
                }
            }

            return arrayOfDetails;
        }

    }

    private static class Details{

        String Text;
        List<String[][]> Tables;
        String Xcoordinates;
        String RowPartitions;
        double[][] TableVerticalCoord; // Horizontal is same for even page, see : PART 1
        double[][] TableHorizontCoord;
        double[][][] TableAllPoints;

    }

    private static class Details_new {
        private int pageNo;
        private String[][] Tables;
        private double[] TableAllPoints;

        public int getPageNo() {
            return pageNo;
        }

        public void setPageNo(int pageNo) {
            this.pageNo = pageNo;
        }

        public String[][] getTables() {
            return Tables;
        }

        public void setTables(String[][] tables) {
            Tables = tables;
        }

        public double[] getTableAllPoints() {
            return TableAllPoints;
        }

        public void setTableAllPoints(double[] tableAllPoints) {
            TableAllPoints = tableAllPoints;
        }
    }


    /*
     *  Used in methods derived from DrawPrintTextLocations
     */
    private AffineTransform flipAT;
    private AffineTransform rotateAT;

    /**
     *  Regions updated by calls to writeString
     */
    private Set<Rectangle2D> boxes;

    // Border to allow when finding intersections
    private double dx = 1.0; // This value works for me, feel free to tweak (or add setter)
    private double dy = 0.000; // Rows of text tend to overlap, so need to extend

    /**
     *  Region in which to find table (otherwise whole page)
     */
    private Rectangle2D regionArea;

    /**
     * Number of rows in inferred table
     */
    private int nRows=0;

    /**
     * Number of columns in inferred table
     */
    private int nCols=0;

    /**
     * This is the object that does the text extraction
     */
    private PDFTextStripperByArea regionStripper;

    /**
     * 1D intervals - used for calculateTableRegions()
     * @author Beldaz
     *
     */
    public static class Interval {
        double start;
        double end;
        public Interval(double start, double end) {
            this.start=start; this.end = end;
        }
        public void add(Interval col) {
            if(col.start<start)
                start = col.start;
            if(col.end>end)
                end = col.end;
        }
        public static void addTo(Interval x, LinkedList<Interval> columns) {
            int p = 0;
            Iterator<Interval> it = columns.iterator();
            // Find where x should go
            while(it.hasNext()) {
                Interval col = it.next();
                if(x.end>=col.start) {
                    if(x.start<=col.end) { // overlaps
                        x.add(col);
                        it.remove();
                    }
                    break;
                }
                ++p;
            }
            while(it.hasNext()) {
                Interval col = it.next();
                if(x.start>col.end)
                    break;
                x.add(col);
                it.remove();
            }
            columns.add(p, x);
        }

    }


    /**
     * Instantiate a new PDFTableStripper object.
     *
     * @param document
     * @throws IOException If there is an error loading the properties.
     */
    public PDFTableStripper() throws IOException
    {
        super.setShouldSeparateByBeads(false);
        regionStripper = new PDFTextStripperByArea();
        regionStripper.setSortByPosition( true );
    }

    /**
     * Define the region to group text by.
     *
     * @param rect The rectangle area to retrieve the text from.
     */
    public void setRegion(Rectangle2D rect )
    {
        regionArea = rect;
    }

    public int getRows()
    {
        return nRows;
    }

    public int getColumns()
    {
        return nCols;
    }

    /**
     * Get the text for the region, this should be called after extractTable().
     *
     * @return The text that was identified in that region.
     */
    public String getText(int row, int col)
    {
        return regionStripper.getTextForRegion("el"+col+"x"+row);
    }

    public Rectangle2D[][] extractTable(PDPage pdPage) throws IOException
    {
        setStartPage(getCurrentPageNo());
        setEndPage(getCurrentPageNo());

        boxes = new HashSet<Rectangle2D>();
        // flip y-axis
        flipAT = new AffineTransform();
        flipAT.translate(0, pdPage.getBBox().getHeight());
        flipAT.scale(1, -1);

        // page may be rotated
        rotateAT = new AffineTransform();
        int rotation = pdPage.getRotation();
        if (rotation != 0)
        {
            PDRectangle mediaBox = pdPage.getMediaBox();
            switch (rotation)
            {
                case 90:
                    rotateAT.translate(mediaBox.getHeight(), 0);
                    break;
                case 270:
                    rotateAT.translate(0, mediaBox.getWidth());
                    break;
                case 180:
                    rotateAT.translate(mediaBox.getWidth(), mediaBox.getHeight());
                    break;
                default:
                    break;
            }
            rotateAT.rotate(Math.toRadians(rotation));
        }
        // Trigger processing of the document so that writeString is called.
        try (Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream())) {
            super.output = dummy;
            super.processPage(pdPage);
        }

        Rectangle2D[][] regions = calculateTableRegions();

        System.err.println("Drawing " + nCols + "x" + nRows + "="+ nRows*nCols + " regions");
        for(int i=0; i<nCols; ++i) {
            for(int j=0; j<nRows; ++j) {
                final Rectangle2D region = regions[i][j];
                regionStripper.addRegion("el"+i+"x"+j, region);
            }
        }

        regionStripper.extractRegions(pdPage);
        return regions;
    }

    /**
     * Infer a rectangular grid of regions from the boxes field.
     *
     * @return 2D array of table regions (as Rectangle2D objects). Note that
     * some of these regions may have no content.
     */
    private Rectangle2D[][] calculateTableRegions() throws IOException {

        // Build up a list of all table regions, based upon the populated
        // regions of boxes field. Treats the horizontal and vertical extents
        // of each box as distinct
        LinkedList<Interval> columns = new LinkedList<Interval>();
        LinkedList<Interval> rows = new LinkedList<Interval>();
        int r = 0;

        int minx = 10000;
        int miny = 10000;
        int maxx = 0;
        int maxy = 0;

        for(Rectangle2D box: boxes) {
            Interval x = new Interval(box.getMinX(), box.getMaxX());
            Interval y = new Interval(box.getMinY(), box.getMaxY());
//            System.out.println(box+"++++++++++++++++++++++++" + x +"**" + y);
            r = r+1;
            Interval.addTo(x, columns);
            Interval.addTo(y, rows);
        }

        nRows = rows.size();
        nCols = columns.size();
        Rectangle2D[][] regions = new Rectangle2D[nCols][nRows];

        int i=0;
        // Label regions from top left, rather than the transformed orientation
        for(Interval column: columns) {
            int j=0;
            for(Interval row: rows) {
                regions[nCols-i-1][nRows-j-1] = new Rectangle2D.Double(column.start, row.start, column.end - column.start, row.end - row.start);
//                System.out.println(regions[nCols-i-1][nRows-j-1]);
                ++j;
            }
            ++i;
        }

        return regions;
    }

    /**
     * Register each character's bounding box, updating boxes field to maintain
     * a list of all distinct groups of characters.
     *
     * Overrides the default functionality of PDFTextStripper.
     * Most of this is taken from DrawPrintTextLocations.java, with extra steps
     * at end of main loop
     */
    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException
    {
        for (TextPosition text : textPositions)
        {
            // glyph space -> user space
            // note: text.getTextMatrix() is *not* the Text Matrix, it's the Text Rendering Matrix
            AffineTransform at = text.getTextMatrix().createAffineTransform();
            PDFont font = text.getFont();
            BoundingBox bbox = font.getBoundingBox();

            // advance width, bbox height (glyph space)
            float xadvance = font.getWidth(text.getCharacterCodes()[0]); // todo: should iterate all chars
            Rectangle2D.Float rect = new Rectangle2D.Float(0, bbox.getLowerLeftY(), xadvance, bbox.getHeight());

            if (font instanceof PDType3Font)
            {
                // bbox and font matrix are unscaled
                at.concatenate(font.getFontMatrix().createAffineTransform());
            }
            else
            {
                // bbox and font matrix are already scaled to 1000
                at.scale(1/1000f, 1/1000f);
            }
            Shape s = at.createTransformedShape(rect);
            s = flipAT.createTransformedShape(s);
            s = rotateAT.createTransformedShape(s);


            //
            // Merge character's bounding box with boxes field
            //
            Rectangle2D bounds = s.getBounds2D();
            // Pad sides to detect almost touching boxes
            Rectangle2D hitbox = bounds.getBounds2D();
            hitbox.add(bounds.getMinX() - dx , bounds.getMinY() - dy);
            hitbox.add(bounds.getMaxX() + dx , bounds.getMaxY() + dy);

            // Find all overlapping boxes
            List<Rectangle2D> intersectList = new ArrayList<Rectangle2D>();
            for(Rectangle2D box: boxes) {
                if(box.intersects(hitbox)) {
                    intersectList.add(box);
                }
            }

            // Combine all touching boxes and update
            // (NOTE: Potentially this could leave some overlapping boxes un-merged,
            // but it's sufficient for now and get's fixed up in calculateTableRegions)
            for(Rectangle2D box: intersectList) {
                bounds.add(box);
                boxes.remove(box);
            }
            boxes.add(bounds);

        }

    }

    /**
     * This method does nothing in this derived class, because beads and regions are incompatible. Beads are
     * ignored when stripping by area.
     *
     * @param aShouldSeparateByBeads The new grouping of beads.
     */
    @Override
    public final void setShouldSeparateByBeads(boolean aShouldSeparateByBeads)
    {
    }

    /**
     * Adapted from PDFTextStripperByArea
     * {@inheritDoc}
     */
    @Override
    protected void processTextPosition( TextPosition text )
    {
        if(regionArea!=null && !regionArea.contains( text.getX(), text.getY() ) ) {
            // skip character
        } else {
            super.processTextPosition( text );
        }
    }
}

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
        Details D0 = getDetails("/home/theperson/IdeaProjects/proj1/src/com/company/Invoice.pdf");
        for (int i =0; i<10; i++){
            System.out.println("________________________________________________" );
            System.out.println("Row: " + i );
            for (int j=0; j<9; j++){
                System.out.println("Column: " + j );
                System.out.println(D0.Tables.get(0)[i][j]);
            }
        }
//        System.out.println("Followingh is the content of the 1st table: \n" + D0.Tables.get(0)[9][0]);
//        System.out.println("Folloeing is the text on the table: \n" + D0.Text);

    }

    public static Details getDetails(String filename) throws IOException {

        // Some helper variables are declared
        double[] rowCoordinates;
        double[] rowHeights;
        int[] rowPage;

        // PDF units are at 72 DPI
        // This number changes with the quality of the pdf
        final double res = 72;

        PDFTableStripper stripper = new PDFTableStripper();
        stripper.setSortByPosition(true);

        try (PDDocument document = PDDocument.load(new File(filename)))
        {
            // ****************** PART 1 ******************************************************
            // Extract TEXT data from each page on the pdf


            // 9x9 inch area is considered on each page
            // Overflow throws no error
            stripper.setRegion(new Rectangle((int) Math.round(0.0*res), (int) Math.round(1*res), (int) Math.round(9*res), (int) Math.round(9.0*res)));

            // Calculating the total number of rows (all pages)
            int totalNoOfRows = 0;
            for (int page = 0; page < document.getNumberOfPages(); ++page)
            {
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
            // This part is only for experimentation of a new algorithm
            // Hence, the result hasn't yet been used anywhere

            // Throughout a column, X is same
            // Array to save the coordinate where each columnis separated from the next
            double[][] rowPartitions = new double[rowCoordinates.length][highestActualNumOfCol+1];

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
                    if (!found2 && x2!=0.0){
                        rowPartitions[i][firstNullElem] = x2;
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

            // ****************** PART 4 ******************************************************
            // Finding the Heading rows (start and end)
            // i.e. finding rows with certain words like "Sl. No." or "Description" (more words need to be added)

            // Array and pointer, for keeping track of all heading rows
            int[] rowsWithHeadingsStart = new int[rowCoordinates.length];
            int[] rowsWithHeadingsEnd = new int[rowCoordinates.length];
            int headingFoundStartPointer = 0;
            int headingFoundEndPointer = 0;

            // Iterating through all rows
            for (int i = 0; i<rowCoordinates.length; i++){
                // Flags to see if headings have been found in the row
                boolean headingFoundStart = false;
                boolean headingFoundEnd = false;
                for (int j = 0; j<highestActualNumOfCol; j++){
                    String content = rowColumnWiseContent[i][j];
                    if (content != null){
                        if( content.contains("Sl.") || content.contains("Description") || content.contains("Title")){
                            headingFoundStart = true;
                            break;
                        }
                        else if(content.contains("Total") || content.contains("TOTAL")){
                            headingFoundEnd = true;
                            break;
                        }
                    }
                }
                // Checking if headings are present
                if (headingFoundStart){
                    rowsWithHeadingsStart[headingFoundStartPointer] = i;
                    headingFoundStartPointer++;
                }
                else if (headingFoundEnd){
                    rowsWithHeadingsEnd[headingFoundEndPointer] = i;
                    headingFoundEndPointer++;
                }
            }

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


            // ****************** PART 5 ******************************************************
            // Creating the array of strings for all tables
            // Each string element has the rows separated by "^^^^^^" , which is in turn separated by "<<<>>>"

            // Keeping a track of the rows that are already a part of some table or the other, is important
            String[] Tables = new String[headingFoundStartPointer];
            boolean[] rowInTable = new boolean[totalNoOfRows];
            double startCoord;
            double endCoord;
            double lastRow;
            int i;

            // List for coordinates
            double[][] TableY1Y2 = new double[headingFoundStartPointer][2];
            double[][] TableX1X2 = new double[headingFoundStartPointer][2];
            double[][][] TableXY = new double[headingFoundStartPointer][4][2];
            List<String[][]> tableContents_l = new ArrayList<String[][]>();

            // Iterating through each pair of headings
            // i.e. iterating through all tables
            for (i =0; i<headingFoundStartPointer; i++) {

                // Creating a temporary 2D array of strings, for each table
                String[][] tempTable = new String[rowsWithHeadingsEnd[i]-rowsWithHeadingsStart[i]+1][highestActualNumOfCol];

                // Coordinates of the table
                startCoord = rowCoordinates[rowsWithHeadingsStart[i]]; // Upper edge of start-heading row
                endCoord = rowCoordinates[rowsWithHeadingsEnd[i]];     // Upper edge of end-heading row
                lastRow = rowCoordinates[rowsWithHeadingsEnd[i] + 1];  // Loweredge of end-heding row

                // Preparing the coordinates DS for "Details" object
                TableX1X2[i][0] = startCoord;
                TableX1X2[i][1] = endCoord;

                // Stripping the region
                // Isolating each table
                double height = endCoord - startCoord;
                stripper.setRegion(new Rectangle((int) Math.round(0.0 * res), (int) Math.round(startCoord), (int) Math.round(9 * res), (int) Math.round(height)));

                //
                String tableContents = "";

                // Getting page of the row
                int page = rowPage[rowsWithHeadingsStart[i]];
                PDPage pdPage = document.getPage(page);

                // Extracting table from the pdf
                // Storing the return 2D array of regions
                Rectangle2D[][] regions = stripper.extractTable(pdPage);

                // Arrays to keep track of Y coordinates, heights, and page numbers
                rowCoordinates = new double[stripper.getRows()];
                rowHeights = new double[stripper.getRows()];
                rowPage = new int[stripper.getRows()];

                // Keeping track of the rows that have been included in
                for (int l = rowsWithHeadingsStart[i]; l<=rowsWithHeadingsEnd[i]; l++){
                    rowInTable[l] = true;
                }

                // Array to store partitions of the first row
                double[] partitions = new double[highestActualNumOfCol];
                double maxX = 0;

                // Stripping each row of the table, into columns
                for (int c = 0; c < stripper.getColumns(); ++c) {
                    tableContents =  tableContents + "^^^^^^\n";
                    for (r = 0; r < stripper.getRows(); ++r) {
                        Rectangle2D region = regions[c][r];
                        rowCoordinates[r] = region.getMinY();
                        rowHeights[r] = region.getHeight();
                        rowPage[r] = page;
                        if (r == 0){
                            partitions[c] = region.getMinX();
                        }
                        if (region.getMaxX()>maxX){
                            maxX = region.getMaxX();
                        }
                        tableContents = tableContents +  "\n<<<>>>" + stripper.getText(r, c);
                        tempTable[r][c] = stripper.getText(r,c);
                    }
                }


                // Extracting table's last row (end-heading row) from the pdf
                // Storing the return 2D array of regions
                int page0 = 0;
                PDPage pdPage0 = document.getPage(page0);

                // Stripping the region
                // Isolating the last row of the table
                stripper.setRegion(new Rectangle((int) Math.round(0.0 * res), (int) Math.round(endCoord), (int) Math.round(9 * res), (int) Math.round(lastRow-endCoord)));
                Rectangle2D[][] regions_0 = stripper.extractTable(pdPage0);
                tableContents += "______\n";
                double[] partitions0 = new double[stripper.getColumns()];
                for (int c = 0; c < stripper.getColumns(); ++c) {
                    for (r = 0; r < 1; ++r) {
                        Rectangle2D region = regions_0[c][r];
                        partitions0[c] = region.getMinX();
                        if (region.getMaxX()>maxX){
                            maxX = region.getMaxX();
                        }
                        tableContents = tableContents +  "\n<<<>>>" + stripper.getText(r, c);
                    }
                }

                // The last row (end-heading row) is extracted separately
                // We compare its partitions with the partitions of the first row (start-heading row)
                // Nearest columns are assumed for each partition of this row
                for (int c0 = 0; c0<stripper.getColumns(); c0++){
                    double lowestDiff = Math.abs(partitions[0] - partitions0[c0]);
                    int nearest = 0; // Column partition (of start heading row) nearest to the currect partition of the end-heading row
                    for (int c=0; c<highestActualNumOfCol;c++){
                        double diff = Math.abs(partitions[c] - partitions0[c0]);
                        if (diff<lowestDiff) {
                            nearest = c;
                            lowestDiff = diff;
                        }
                    }
                    r = rowsWithHeadingsEnd[i];
                    tempTable[r-rowsWithHeadingsStart[i]][nearest] = stripper.getText(0,c0);
                }
                TableY1Y2[i][1] = maxX;

                // Preparing the array of coordinates for the "Details" object
                TableXY[i][0][0] = TableX1X2[i][0];
                TableXY[i][0][1] = TableY1Y2[i][0];
                TableXY[i][1][0] = TableX1X2[i][1];
                TableXY[i][1][1] = TableY1Y2[i][0];
                TableXY[i][2][0] = TableX1X2[i][0];
                TableXY[i][2][1] = TableY1Y2[i][1];
                TableXY[i][3][0] = TableX1X2[i][1];
                TableXY[i][3][1] = TableY1Y2[i][1];

                // Following Data structure is created for debugging purposes
                Tables[i] = tableContents;
                Tables[i] = tableContents;

                // Adding the temporary table to the listof all tables
                tableContents_l.add(tempTable);

//                 Following commented snippet is used for debugging purposes only
//                System.out.println("*******************");
//                for (r = 0; r<rowsWithHeadingsEnd[i]-rowsWithHeadingsStart[i] +1; r++){
//                    System.out.println("&&&");
//                    for (int c =0; c<highestActualNumOfCol; c++){
//                        System.out.println(tableContents_l.get(i)[r][c]);
//                    }
//                }
            }

            // Preparing the Details object that has to be returned (this is for debugging after extraction)
            String Text = "";
            String Xcoordinates = "";
            String RowPartitions = "";
            int j;
            for (i = 0; i<rowInTable.length; i++){
                if (!rowInTable[i]){
                    Text = Text + "^^^^^^\n";
                    Xcoordinates = Xcoordinates + "^^^^^^\n";
                    RowPartitions = RowPartitions + "^^^^^^\n";
                    for (j=0; j<highestActualNumOfCol; j++){
                        Text = Text + "\n<<<>>>" + rowColumnWiseContent[i][j];
                        Xcoordinates = "\n<<<>>>" + rowCooordHeight[i][j][0] + "___" + rowCooordHeight[i][j][1];
                        RowPartitions = "\n<<<>>>" + rowPartitions[i][j];
                    }
                }
            }

            // ****************** PART 5 ******************************************************
            // Finally, creating the "Details" object, returned in the "getDetails()" function
            Details D = new Details();
            D.Text = Text;
            D.Tables = tableContents_l;
            D.Xcoordinates = Xcoordinates;
            D.RowPartitions = RowPartitions;
            D.TableVerticalCoord = TableX1X2;
            D.TableHorizontCoord = TableY1Y2;
            D.TableAllPoints = TableXY;

            return D;
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

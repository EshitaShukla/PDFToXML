/*
 * Copyright 2017 Beldaz (https://github.com/beldaz)
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
     * This will print the documents data, for each table cell.
     *
     * @param args The command line arguments.
     *
     * @throws IOException If there is an error parsing the document.
     */
    public static void main(String[] args) throws IOException
    {
        // Function to get all data from the pdf
        Details D0 = getDetails("/home/theperson/IdeaProjects/proj1/src/com/company/Invoice.pdf");
        System.out.println("Followingh is the content of the 1st table: \n" + D0.Tables[0]);

    }

    public static Details getDetails(String filename) throws IOException {

        double[] row_coordinates = new double[0];
        double[] row_heights = new double[0];
        int[] row_page = new int[0];
//        String fname = "/home/theperson/IdeaProjects/proj1/src/com/company/Invoice.pdf";

        try (PDDocument document = PDDocument.load(new File(filename)))
        {
            // ****************** PART 1 ******************************************************
            // Extract TEXT data from each page on the pdf

            // PDF units are at 72 DPI
            // This number changes with the quality of the pdf
            final double res = 72;
            PDFTableStripper stripper = new PDFTableStripper();
            stripper.setSortByPosition(true);

            // 9x9 inch area is considered on each page
            // Overflow throws no error
            stripper.setRegion(new Rectangle((int) Math.round(0.0*res), (int) Math.round(1*res), (int) Math.round(9*res), (int) Math.round(9.0*res)));

            int total_no_of_rows = 0;
            for (int page = 0; page < document.getNumberOfPages(); ++page)
            {
//                System.out.println("Page " + page);
                PDPage pdPage = document.getPage(page);
                Rectangle2D[][] regions = stripper.extractTable(pdPage);


                int R = stripper.getRows();
                total_no_of_rows +=R;
            }

            row_coordinates = new double[total_no_of_rows];
            row_heights = new double[total_no_of_rows];
            row_page = new int[total_no_of_rows];

            int R = 0;
            // Extract data from each page on the pdf
            for (int page = 0; page < document.getNumberOfPages(); ++page)
            {
//                System.out.println("Page " + page);
                PDPage pdPage = document.getPage(page);
                Rectangle2D[][] regions = stripper.extractTable(pdPage);

                for(int r=0; r<stripper.getRows(); ++r) {

                    for(int c=0; c<stripper.getColumns(); ++c) {
                        Rectangle2D region = regions[c][r];
                        row_coordinates[R] = region.getMinY();
                        row_heights[R] = region.getHeight();
                        row_page[R] = page;
                    }
                    R +=1;
                }
            }


            int r = 0;
            double[][][] row_cooord_height = new double[row_coordinates.length][15][2];
            String[][] row_column_wise_content = new String[row_coordinates.length][15];
            for (int i = 0; i< row_coordinates.length; i++){

                stripper = new PDFTableStripper();
                stripper.setSortByPosition(true);


                // Choose a region in which to extract a table (here a 6"wide, 9" high rectangle offset 1" from top left of page)
                stripper.setRegion(new Rectangle((int) Math.round(0.0*res), (int) Math.round(row_coordinates[i]), (int) Math.round(9*res), (int) Math.round(row_heights[i])));

                // Repeat for each page of PDF
                int page = row_page[i];
                PDPage pdPage = document.getPage(page);
                Rectangle2D[][] regions = stripper.extractTable(pdPage);

                for(int c=0; c<stripper.getColumns(); ++c) {
                    Rectangle2D region = regions[c][r];
                    row_coordinates[r] = region.getMinY();
                    String text = stripper.getText(r, c);
                    row_column_wise_content[i][c] = text;
                    row_cooord_height[i][c][0] = region.getMinX();
                    row_cooord_height[i][c][1] = region.getMaxX();
                }
            }

            // ****************** PART 2 ******************************************************
            // Using row coordinates calculated above, divide the pdf into rectangles
            // Extract all contents from each rectangle , and partition the content into columns
            // i is row number; c is column number

            int highest_actual_num_of_col = 0;
            for (int i = 0; i<row_coordinates.length; i++){
                int actual_num_of_col = 0;
                for (int c = 0; c<10; c++){

                    if (row_column_wise_content[i][c] != null){
                        actual_num_of_col = actual_num_of_col +1;
                    }
                }
                if (actual_num_of_col>highest_actual_num_of_col){
                    highest_actual_num_of_col = actual_num_of_col;
                }
            }

            //Throughout a column x is same
            // Grouping rows together, that are a part of a table i.e. assigning each row to a table based on partions inside a row
            double[][] row_partitions = new double[row_coordinates.length][highest_actual_num_of_col+1];

            for (int i = 0; i<row_coordinates.length; i++){
                for (int j=0; j<highest_actual_num_of_col; j++){

                    double x1 = row_cooord_height[i][j][0];
                    double x2 = row_cooord_height[i][j][1];

                    boolean found1 = false;
                    boolean found2 = false;

                    int first_null_elem = 0;
                    for (int k=0; k<highest_actual_num_of_col;k++){
                        if (row_partitions[i][k] == 0.0){
                            first_null_elem = k;
                            break;
                        }
                        if (row_partitions[i][k] == x1){
                            found1 = true;
                        }
                        if (row_partitions[i][k] == x2){
                            found2 = true;
                        }
                        if (found1 && found2){
                            break;
                        }
                    }
                    if (!found1 && x1!=0.0){
                        row_partitions[i][first_null_elem] = x1;
                        first_null_elem = first_null_elem + 1;
                    }
                    if (!found2 && x2!=0.0){
                        row_partitions[i][first_null_elem] = x2;
                    }
                }
            }

//            System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
//            for (int i = 0; i<row_coordinates.length; i++) {
//                System.out.println("Row: " + i);
//                for (int j = 0; j < highest_actual_num_of_col; j++) {
//                    System.out.println(row_partitions[i][j]);
//                }
//            }

            // ****************** PART 3 ******************************************************
            // Finding the Heading rows(start and end)
            // i.e. finding rows with certain words

            int[] rows_with_headings__start = new int[row_coordinates.length];
            int[] rows_with_headings__end = new int[row_coordinates.length];
            int heading_found__start_pointer = 0;
            int heading_found__end_pointer = 0;
            for (int i = 0; i<row_coordinates.length; i++){
                boolean heading_found__start = false;
                boolean heading_found__end = false;
                for (int j = 0; j<highest_actual_num_of_col; j++){
                    String content = row_column_wise_content[i][j];
                    if (content != null){
                        if( content.contains("Sl.") || content.contains("Description") || content.contains("Title")){
                            heading_found__start = true;
                            break;
                        }
                        else if(content.contains("Total") || content.contains("TOTAL")){
                            heading_found__end = true;
                            break;
                        }
                    }
                }
                if (heading_found__start){
                    rows_with_headings__start[heading_found__start_pointer] = i;
                    heading_found__start_pointer++;
                }
                else if (heading_found__end){
                    rows_with_headings__end[heading_found__end_pointer] = i;
                    heading_found__end_pointer++;
                }

            }

            int[][] table0= new int[heading_found__start_pointer][row_coordinates.length];
            for (int i = 0; i<heading_found__start_pointer; i++){
                for (int j =rows_with_headings__start[i]; j<=rows_with_headings__end[i]; j++){
                    String S = "";
                    for (int k=0; k<highest_actual_num_of_col; k++){
                        if (row_column_wise_content[j][k] !=null){
                            S = S+row_column_wise_content[j][k];

                        }
                    }
                    table0[i][j-rows_with_headings__start[i]] = j;
                }
            }

//            System.out.println("%%%%%%%%%%%%%%%%%%%%%%");

            // ****************** PART 4 ******************************************************
            // Creating the array of strings for all tables
            // Each string element has the rows separated by "^^^^^^" , which is in turn separated by "<<<>>>"

            // Keeping a track of the rows that are already a part of some table or the other, is important
            String[] Tables = new String[heading_found__start_pointer];
            boolean[] row_in_table = new boolean[total_no_of_rows];
            double start_coord;
            double end_coord;
            int i;

            // Iterating through each pair of headings
            // i.e. iterating through all tables
            for (i =0; i<heading_found__start_pointer; i++) {
                start_coord = row_coordinates[rows_with_headings__start[i]];
                end_coord = row_coordinates[rows_with_headings__end[i]];
                double height = end_coord - start_coord;
                stripper.setRegion(new Rectangle((int) Math.round(0.0 * res), (int) Math.round(start_coord), (int) Math.round(9 * res), (int) Math.round(height)));

                String table_contents = "";
                int page = 0;
                PDPage pdPage = document.getPage(page);
                Rectangle2D[][] regions = stripper.extractTable(pdPage);
                row_coordinates = new double[stripper.getRows()];
                row_heights = new double[stripper.getRows()];
                row_page = new int[stripper.getRows()];

                for (int l = rows_with_headings__start[i]; l<=rows_with_headings__end[i]; l++){
                    row_in_table[l] = true;
                }


                for (int c = 0; c < stripper.getColumns(); ++c) {
                    table_contents =   "^^^^^^\n"+table_contents;
                    for (r = 0; r < stripper.getRows(); ++r) {
                        Rectangle2D region = regions[c][r];
                        row_coordinates[r] = region.getMinY();
                        row_heights[r] = region.getHeight();
                        row_page[r] = page;
                        table_contents = table_contents +  "\n<<<>>>" + stripper.getText(r, c);
                    }
                }
                Tables[i] = table_contents;

            }


            // Preparing the Details object that has to be returned
            String Text = "";
            String Xcoordinates = "";
            String RowPartitions = "";
            int j;
            for (i = 0; i<row_in_table.length; i++){
                if (!row_in_table[i]){
                    Text = Text + "^^^^^^\n";
                    Xcoordinates = Xcoordinates + "^^^^^^\n";
                    RowPartitions = RowPartitions + "^^^^^^\n";
                    for (j=0; j<highest_actual_num_of_col; j++){
                        Text = Text + "\n<<<>>>" + row_column_wise_content[i][j];
                        Xcoordinates = "\n<<<>>>" + row_cooord_height[i][j][0] + "___" + row_cooord_height[i][j][1];
                        RowPartitions = "\n<<<>>>" + row_partitions[i][j];
                    }
                }
            }

            Details D = new Details();
            D.Text = Text;
            D.Tables = Tables;
            D.Xcoordinates = Xcoordinates;
            D.RowPartitions = RowPartitions;

            return D;
        }

    }

    private static class Details{

        String Text;
        String[] Tables;
        String Xcoordinates;
        String RowPartitions;

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

package report;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import db.DBconnection;

import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

import static literals.Literals.dbcon;

public class ReportPdf {
    private static String FILE = "F:\\Sem 3\\Group-08WSE\\HawkHunt\\src\\report\\Report.pdf";

    public static void main(String[] args) {
        try {
            System.out.println("Enter n:");
            Scanner reader = new Scanner(System.in);
            int n = reader.nextInt();
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, new FileOutputStream(FILE)).setPageCount(1);
            document.open();
            addContent(document, n);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addContent(Document document, int n) throws Exception {
        Chapter catPart = new Chapter(1);
        Section subCatPart = catPart;
        Paragraph paragraph = new Paragraph();
        subCatPart.add(paragraph);
        createTable(subCatPart, n);
        document.add(catPart);

    }

    private static void createTable(Section subCatPart, int n) throws Exception {
        PdfPTable table = new PdfPTable(4);
        PdfPCell c1 = new PdfPCell(new Phrase("Average"));
        c1.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c1);

        c1 = new PdfPCell(new Phrase("Median"));
        c1.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c1);

        c1 = new PdfPCell(new Phrase("First Quartile"));
        c1.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c1);

        c1 = new PdfPCell(new Phrase("Third Quartile"));
        c1.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c1);

        table.setHeaderRows(1);
        double avg = average(n);
        double med = median(n);
        double firstQ = firstQuartile(n);
        double thirdQ = thirdQuartile(n);
        table.addCell("" + avg + "");
        table.addCell("" + med + "");
        table.addCell("" + firstQ + "");
        table.addCell("" + thirdQ + "");
        float[] columnWidths = new float[]{50f, 50f, 50f, 50f};
        table.setWidths(columnWidths);
        subCatPart.add(table);
    }

    private static double average(int n) {
        double averag = 0;
        try {

            Connection conn = DBconnection.getCon(dbcon, true);
            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery("SELECT avg(jacardsim-jaccard" + n + ") FROM jaccardsim;");
            while (resultSet.next()) {
                averag = resultSet.getDouble(1);
            }
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return averag;
    }

    private static double median(int n) {
        double averag = 0;
        try {

            Connection conn = DBconnection.getCon(dbcon, true);
            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery("SELECT percentile_cont(0.5) WITHIN GROUP (ORDER BY jacardsim-jaccardsim.jaccard"+n+") FROM jaccardsim;");
            while (resultSet.next()) {
                averag = resultSet.getDouble(1);
            }
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return averag;
    }

    private static double firstQuartile(int n) {
        double firstQuar = 0;
        try {

            Connection conn = DBconnection.getCon(dbcon, true);
            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery("SELECT percentile_cont(0.25) WITHIN GROUP (ORDER BY jacardsim-jaccardsim.jaccard"+n+") FROM jaccardsim;");
            while (resultSet.next()) {
                firstQuar = resultSet.getDouble(1);
            }
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return firstQuar;
    }

    private static double thirdQuartile(int n) {
        double thirdQuar = 0;
        try {

            Connection conn = DBconnection.getCon(dbcon, true);
            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery("SELECT percentile_cont(0.75) WITHIN GROUP (ORDER BY jacardsim-jaccardsim.jaccard"+n+") FROM jaccardsim;");
            while (resultSet.next()) {
                thirdQuar = resultSet.getDouble(1);
            }
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return thirdQuar;
    }
}
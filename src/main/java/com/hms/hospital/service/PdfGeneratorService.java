package com.hms.hospital.service;

import com.hms.hospital.entity.Appointment;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGeneratorService {

    public ByteArrayInputStream generateAppointmentReceipt(Appointment appointment) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font successFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(16, 124, 65));

            // Header Section
            Paragraph hospitalName = new Paragraph("CITY LIFE GENERAL HOSPITAL", titleFont);
            hospitalName.setAlignment(Element.ALIGN_CENTER);
            document.add(hospitalName);

            Paragraph hospitalSub = new Paragraph("123 Healthcare Boulevard, Medical Enclave, City • Contact: +91 98765 43210 • Email: billing@citylifehospital.com", subtitleFont);
            hospitalSub.setAlignment(Element.ALIGN_CENTER);
            document.add(hospitalSub);

            Paragraph divider = new Paragraph("_________________________________________________________________________________", subtitleFont);
            divider.setAlignment(Element.ALIGN_CENTER);
            document.add(divider);
            document.add(Chunk.NEWLINE);

            // Receipt Title & Status Box
            PdfPTable receiptHeadTable = new PdfPTable(2);
            receiptHeadTable.setWidthPercentage(100);

            PdfPCell cellLeft = new PdfPCell(new Phrase("MEDICAL CONSULTATION RECEIPT / BILL", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(30, 41, 59))));
            cellLeft.setBorder(Rectangle.NO_BORDER);

            String payStatusStr = appointment.getPaymentStatus() != null ? appointment.getPaymentStatus().toUpperCase() : "PAID";
            PdfPCell cellRight = new PdfPCell(new Phrase("STATUS: " + payStatusStr, successFont));
            cellRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellRight.setBorder(Rectangle.NO_BORDER);

            receiptHeadTable.addCell(cellLeft);
            receiptHeadTable.addCell(cellRight);
            document.add(receiptHeadTable);
            document.add(Chunk.NEWLINE);

            // Details Grid (Patient & Doctor Info)
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(10f);
            infoTable.setSpacingAfter(10f);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

            String patientName = appointment.getPatient() != null ? appointment.getPatient().getName() : "N/A";
            String patientIdStr = appointment.getPatient() != null ? appointment.getPatient().getPatientId() : "N/A";
            String doctorName = appointment.getDoctor() != null ? appointment.getDoctor().getName() : "N/A";
            String doctorSpec = (appointment.getDoctor() != null && appointment.getDoctor().getSpecialization() != null) 
                    ? appointment.getDoctor().getSpecialization() : "General Physician";
            
            String aptDate = appointment.getStartTime() != null ? appointment.getStartTime().format(formatter) : "N/A";
            String payDate = appointment.getPaymentDate() != null ? appointment.getPaymentDate().format(formatter) : aptDate;
            String txnId = appointment.getPaymentId() != null ? appointment.getPaymentId() : ("TXN-" + System.currentTimeMillis() % 1000000);
            String payMode = appointment.getPaymentMode() != null ? appointment.getPaymentMode() : "UPI QR Code";

            addInfoRow(infoTable, "Bill / Receipt No:", "BILL-APT-" + appointment.getId(), boldFont, normalFont);
            addInfoRow(infoTable, "Receipt Date:", payDate, boldFont, normalFont);
            addInfoRow(infoTable, "Patient Name:", patientName + " (" + patientIdStr + ")", boldFont, normalFont);
            addInfoRow(infoTable, "Doctor Name:", "Dr. " + doctorName, boldFont, normalFont);
            addInfoRow(infoTable, "Appointment Time:", aptDate, boldFont, normalFont);
            addInfoRow(infoTable, "Specialization:", doctorSpec, boldFont, normalFont);
            addInfoRow(infoTable, "Appointment Type:", appointment.getAppointmentType() != null ? appointment.getAppointmentType() : "REGULAR", boldFont, normalFont);
            addInfoRow(infoTable, "Transaction ID:", txnId + " (" + payMode + ")", boldFont, normalFont);

            document.add(infoTable);
            document.add(Chunk.NEWLINE);

            // Fee Breakdown Table
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1, 3, 2});

            // Table Header
            PdfPCell h1 = new PdfPCell(new Phrase("S.No", headerFont));
            h1.setBackgroundColor(new Color(30, 58, 138));
            h1.setPadding(8);
            h1.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(h1);

            PdfPCell h2 = new PdfPCell(new Phrase("Description", headerFont));
            h2.setBackgroundColor(new Color(30, 58, 138));
            h2.setPadding(8);
            table.addCell(h2);

            PdfPCell h3 = new PdfPCell(new Phrase("Amount (INR)", headerFont));
            h3.setBackgroundColor(new Color(30, 58, 138));
            h3.setPadding(8);
            h3.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(h3);

            // Table Row
            Double feeAmount = appointment.getAmountPaid();
            if (feeAmount == null && appointment.getDoctor() != null && appointment.getDoctor().getFees() != null) {
                feeAmount = appointment.getDoctor().getFees();
            }
            if (feeAmount == null) {
                feeAmount = 500.0;
            }

            PdfPCell c1 = new PdfPCell(new Phrase("1", normalFont));
            c1.setPadding(8);
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c1);

            PdfPCell c2 = new PdfPCell(new Phrase("Doctor Consultation Fee - Dr. " + doctorName + " (" + doctorSpec + ")", normalFont));
            c2.setPadding(8);
            table.addCell(c2);

            PdfPCell c3 = new PdfPCell(new Phrase(String.format("INR %.2f", feeAmount), normalFont));
            c3.setPadding(8);
            c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(c3);

            // Total Row
            PdfPCell tLabel = new PdfPCell(new Phrase("Total Amount Paid", boldFont));
            tLabel.setColspan(2);
            tLabel.setPadding(8);
            tLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tLabel.setBackgroundColor(new Color(241, 245, 249));
            table.addCell(tLabel);

            PdfPCell tVal = new PdfPCell(new Phrase(String.format("INR %.2f", feeAmount), boldFont));
            tVal.setPadding(8);
            tVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tVal.setBackgroundColor(new Color(241, 245, 249));
            table.addCell(tVal);

            document.add(table);
            document.add(Chunk.NEWLINE);

            // Footer & Signatory
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);

            PdfPCell noteCell = new PdfPCell(new Phrase("Note: This is a computer generated official medical bill receipt. No physical signature is required.", subtitleFont));
            noteCell.setBorder(Rectangle.NO_BORDER);

            PdfPCell signCell = new PdfPCell(new Phrase("\n\n_______________________\nAuthorized Signature\nCity Life General Hospital", boldFont));
            signCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            signCell.setBorder(Rectangle.NO_BORDER);

            footerTable.addCell(noteCell);
            footerTable.addCell(signCell);
            document.add(footerTable);

            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream generateHospitalBillPdf(com.hms.hospital.entity.HospitalBill bill) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(15, 23, 42));
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font successFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(16, 124, 65));

            // Header Section
            Paragraph hospitalName = new Paragraph("CITY LIFE GENERAL HOSPITAL", titleFont);
            hospitalName.setAlignment(Element.ALIGN_CENTER);
            document.add(hospitalName);

            Paragraph hospitalSub = new Paragraph("123 Healthcare Boulevard, Medical Enclave, City • Phone: +91 98765 43210 • Email: billing@citylifehospital.com", subtitleFont);
            hospitalSub.setAlignment(Element.ALIGN_CENTER);
            document.add(hospitalSub);

            Paragraph divider = new Paragraph("_________________________________________________________________________________", subtitleFont);
            divider.setAlignment(Element.ALIGN_CENTER);
            document.add(divider);
            document.add(Chunk.NEWLINE);

            // Receipt Title & Status Box
            PdfPTable receiptHeadTable = new PdfPTable(2);
            receiptHeadTable.setWidthPercentage(100);

            PdfPCell cellLeft = new PdfPCell(new Phrase("OFFICIAL ITEMIZED HOSPITAL RECEIPT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(30, 41, 59))));
            cellLeft.setBorder(Rectangle.NO_BORDER);

            String payStatusStr = bill.getPaymentStatus() != null ? bill.getPaymentStatus().toUpperCase() : "PAID";
            PdfPCell cellRight = new PdfPCell(new Phrase("STATUS: " + payStatusStr, successFont));
            cellRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellRight.setBorder(Rectangle.NO_BORDER);

            receiptHeadTable.addCell(cellLeft);
            receiptHeadTable.addCell(cellRight);
            document.add(receiptHeadTable);
            document.add(Chunk.NEWLINE);

            // Details Grid
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(5f);
            infoTable.setSpacingAfter(10f);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
            String billDateStr = bill.getBillDate() != null ? bill.getBillDate().format(formatter) : "N/A";
            String catMode = (bill.getPaymentCategory() != null ? bill.getPaymentCategory() : "OFFLINE") + " (" + (bill.getPaymentMode() != null ? bill.getPaymentMode() : "Cash") + ")";

            addInfoRow(infoTable, "Bill Receipt No:", bill.getBillNumber(), boldFont, normalFont);
            addInfoRow(infoTable, "Billing Date:", billDateStr, boldFont, normalFont);
            addInfoRow(infoTable, "Patient Name:", bill.getPatientName() != null ? bill.getPatientName() : "N/A", boldFont, normalFont);
            addInfoRow(infoTable, "Patient Phone:", bill.getPatientPhone() != null ? bill.getPatientPhone() : "N/A", boldFont, normalFont);
            addInfoRow(infoTable, "Attending Doctor:", bill.getDoctorName() != null ? bill.getDoctorName() : "General Hospital Duty Doctor", boldFont, normalFont);
            addInfoRow(infoTable, "Payment Mode:", catMode, boldFont, normalFont);
            if (bill.getTransactionId() != null && !bill.getTransactionId().isEmpty()) {
                addInfoRow(infoTable, "Transaction ID:", bill.getTransactionId(), boldFont, normalFont);
            }

            document.add(infoTable);
            document.add(Chunk.NEWLINE);

            // Itemized Services Table
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1, 4, 2});

            // Table Header
            PdfPCell h1 = new PdfPCell(new Phrase("S.No", headerFont));
            h1.setBackgroundColor(new Color(15, 23, 42));
            h1.setPadding(8);
            h1.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(h1);

            PdfPCell h2 = new PdfPCell(new Phrase("Service / Test / Facility Description", headerFont));
            h2.setBackgroundColor(new Color(15, 23, 42));
            h2.setPadding(8);
            table.addCell(h2);

            PdfPCell h3 = new PdfPCell(new Phrase("Amount (INR)", headerFont));
            h3.setBackgroundColor(new Color(15, 23, 42));
            h3.setPadding(8);
            h3.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(h3);

            // Parse services summary
            String summary = bill.getServicesSummary();
            if (summary == null || summary.trim().isEmpty()) {
                summary = "Hospital Treatment & Consultation Services";
            }

            String[] items = summary.split(",");
            int itemNo = 1;
            for (String item : items) {
                if (item.trim().isEmpty()) continue;

                PdfPCell c1 = new PdfPCell(new Phrase(String.valueOf(itemNo++), normalFont));
                c1.setPadding(8);
                c1.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(c1);

                PdfPCell c2 = new PdfPCell(new Phrase(item.trim(), normalFont));
                c2.setPadding(8);
                table.addCell(c2);

                PdfPCell c3 = new PdfPCell(new Phrase("-", normalFont));
                c3.setPadding(8);
                c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(c3);
            }

            // Total Amount Row
            Double total = bill.getTotalAmount() != null ? bill.getTotalAmount() : 0.0;
            PdfPCell tLabel = new PdfPCell(new Phrase("Total Grand Amount Paid", boldFont));
            tLabel.setColspan(2);
            tLabel.setPadding(8);
            tLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tLabel.setBackgroundColor(new Color(241, 245, 249));
            table.addCell(tLabel);

            PdfPCell tVal = new PdfPCell(new Phrase(String.format("INR %.2f", total), boldFont));
            tVal.setPadding(8);
            tVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tVal.setBackgroundColor(new Color(241, 245, 249));
            table.addCell(tVal);

            document.add(table);
            document.add(Chunk.NEWLINE);

            // Footer & Signatory
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);

            PdfPCell noteCell = new PdfPCell(new Phrase("Note: This is an official computer-generated medical bill. Includes all offline/online diagnostics, scans & services.", subtitleFont));
            noteCell.setBorder(Rectangle.NO_BORDER);

            PdfPCell signCell = new PdfPCell(new Phrase("\n\n_______________________\nAccounts & Billing Stamp\nCity Life General Hospital", boldFont));
            signCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            signCell.setBorder(Rectangle.NO_BORDER);

            footerTable.addCell(noteCell);
            footerTable.addCell(signCell);
            document.add(footerTable);

            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addInfoRow(PdfPTable table, String label, String value, Font boldFont, Font normalFont) {
        PdfPCell cell1 = new PdfPCell(new Phrase(label, boldFont));
        cell1.setBorder(Rectangle.NO_BORDER);
        cell1.setPadding(4);
        table.addCell(cell1);

        PdfPCell cell2 = new PdfPCell(new Phrase(value, normalFont));
        cell2.setBorder(Rectangle.NO_BORDER);
        cell2.setPadding(4);
        table.addCell(cell2);
    }
}

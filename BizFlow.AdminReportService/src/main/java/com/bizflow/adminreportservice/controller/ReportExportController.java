package com.bizflow.adminreportservice.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bizflow.adminreportservice.dto.DailySalesDto;
import com.bizflow.adminreportservice.dto.LowStockDto;
import com.bizflow.adminreportservice.dto.SalesOverviewDto;
import com.bizflow.adminreportservice.service.AnalyticsReportService;

@RestController
@RequestMapping("/admin/reports/export")
public class ReportExportController {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	private final AnalyticsReportService analyticsReportService;

	public ReportExportController(AnalyticsReportService analyticsReportService) {
		this.analyticsReportService = analyticsReportService;
	}

	@GetMapping(value = "/sales", produces = "text/csv")
	public ResponseEntity<String> exportSalesCsv(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "csv") String format) {

		// For now we support CSV regardless of format. The UI may pass pdf/excel.
		SalesOverviewDto overview = analyticsReportService.salesOverview(from, to);
		List<DailySalesDto> daily = analyticsReportService.salesDaily(from, to);

		StringBuilder csv = new StringBuilder();
		csv.append("from,to,totalOrders,paidOrders,revenue,averageOrderValue\n");
		csv.append(nullSafe(from)).append(',')
				.append(nullSafe(to)).append(',')
				.append(overview.totalOrders()).append(',')
				.append(overview.paidOrders()).append(',')
				.append(nullSafe(overview.revenue())).append(',')
				.append(nullSafe(overview.averageOrderValue())).append("\n\n");

		csv.append("date,orderCount,revenue\n");
		for (DailySalesDto d : daily) {
			csv.append(d.date() != null ? d.date().format(DATE) : "").append(',')
					.append(d.orderCount()).append(',')
					.append(nullSafe(d.revenue())).append('\n');
		}

		return ResponseEntity.ok()
				.headers(downloadHeaders("sales-report.csv"))
				.contentType(MediaType.valueOf("text/csv"))
				.body(csv.toString());
	}

	@GetMapping(value = "/inventory/low-stock", produces = "text/csv")
	public ResponseEntity<String> exportLowStockCsv(
			@RequestParam(defaultValue = "10") int threshold,
			@RequestParam(defaultValue = "50") int limit,
			@RequestParam(defaultValue = "csv") String format) {

		List<LowStockDto> items = analyticsReportService.lowStock(threshold, limit);

		StringBuilder csv = new StringBuilder();
		csv.append("threshold,limit\n");
		csv.append(threshold).append(',').append(limit).append("\n\n");
		csv.append("productId,productName,stock\n");
		for (LowStockDto i : items) {
			csv.append(i.productId()).append(',')
					.append(escapeCsv(i.productName())).append(',')
					.append(i.stock()).append('\n');
		}

		return ResponseEntity.ok()
				.headers(downloadHeaders("low-stock.csv"))
				.contentType(MediaType.valueOf("text/csv"))
				.body(csv.toString());
	}

	private static HttpHeaders downloadHeaders(String filename) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
		return headers;
	}

	private static String nullSafe(LocalDate d) {
		return d == null ? "" : d.format(DATE);
	}

	private static String nullSafe(BigDecimal v) {
		return v == null ? "0" : v.toPlainString();
	}

	private static String escapeCsv(String s) {
		if (s == null) return "";
		boolean mustQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
		if (!mustQuote) return s;
		return '"' + s.replace("\"", "\"\"") + '"';
	}
}


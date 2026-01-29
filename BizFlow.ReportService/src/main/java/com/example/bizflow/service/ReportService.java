package com.example.bizflow.service;

import com.example.bizflow.dto.DailyReportDTO;
import com.example.bizflow.dto.LowStockAlertDTO;
import com.example.bizflow.dto.RevenueReportDTO;
import com.example.bizflow.dto.TopProductDTO;
import com.example.bizflow.integration.CatalogClient;
import com.example.bizflow.integration.InventoryClient;
import com.example.bizflow.integration.SalesClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final SalesClient salesClient;
    private final CatalogClient catalogClient;
    private final InventoryClient inventoryClient;

    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;
    private static final int CRITICAL_STOCK_THRESHOLD = 5;

    private final Map<Long, BigDecimal> costPriceCache = new HashMap<>();

    public ReportService(SalesClient salesClient,
                         CatalogClient catalogClient,
                         InventoryClient inventoryClient) {
        this.salesClient = salesClient;
        this.catalogClient = catalogClient;
        this.inventoryClient = inventoryClient;
    }

    private BigDecimal getCostPrice(Long productId) {
        if (productId == null) {
            return BigDecimal.ZERO;
        }
        if (costPriceCache.containsKey(productId)) {
            return costPriceCache.get(productId);
        }
        BigDecimal costPrice = catalogClient.getCostPrice(productId);
        if (costPrice == null) {
            costPrice = BigDecimal.ZERO;
        }
        costPriceCache.put(productId, costPrice);
        return costPrice;
    }

    public void clearCostPriceCache() {
        costPriceCache.clear();
    }

    public RevenueReportDTO getRevenueReport(LocalDate startDate, LocalDate endDate, String period) {
        List<SalesClient.OrderSnapshot> orders = salesClient.getOrders(startDate, endDate);

        List<SalesClient.OrderSnapshot> paidOrders = orders.stream()
                .filter(o -> "PAID".equalsIgnoreCase(o.getStatus()))
                .filter(o -> !Boolean.TRUE.equals(o.getReturnOrder()))
                .collect(Collectors.toList());

        RevenueReportDTO report = new RevenueReportDTO();
        report.setPeriod(period);
        report.setStartDate(startDate.format(DateTimeFormatter.ISO_DATE));
        report.setEndDate(endDate.format(DateTimeFormatter.ISO_DATE));
        report.setTotalOrders(paidOrders.size());

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        long totalItemsSold = 0;

        Map<LocalDate, List<SalesClient.OrderSnapshot>> ordersByDate = paidOrders.stream()
                .filter(o -> o.getCreatedAt() != null)
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate()));

        List<RevenueReportDTO.DailyRevenueItem> dailyBreakdown = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<SalesClient.OrderSnapshot> dayOrders = ordersByDate.getOrDefault(date, Collections.emptyList());

            BigDecimal dayRevenue = BigDecimal.ZERO;
            BigDecimal dayCost = BigDecimal.ZERO;

            for (SalesClient.OrderSnapshot order : dayOrders) {
                dayRevenue = dayRevenue.add(safeAmount(order.getTotalAmount()));
                if (order.getItems() != null) {
                    for (SalesClient.OrderItemSnapshot item : order.getItems()) {
                        Integer qtyValue = item.getQuantity();
                        int qty = qtyValue != null ? qtyValue : 0;
                        totalItemsSold += qty;
                        BigDecimal costPrice = getCostPrice(item.getProductId());
                        dayCost = dayCost.add(costPrice.multiply(BigDecimal.valueOf(qty)));
                    }
                }
            }

            totalRevenue = totalRevenue.add(dayRevenue);
            totalCost = totalCost.add(dayCost);

            BigDecimal dayProfit = dayRevenue.subtract(dayCost);
            BigDecimal dayMargin = BigDecimal.ZERO;
            if (dayRevenue.compareTo(BigDecimal.ZERO) > 0) {
                dayMargin = dayProfit.multiply(BigDecimal.valueOf(100))
                        .divide(dayRevenue, 2, RoundingMode.HALF_UP);
            }

            dailyBreakdown.add(new RevenueReportDTO.DailyRevenueItem(
                    date.format(DateTimeFormatter.ISO_DATE),
                    dayRevenue,
                    dayCost,
                    dayProfit,
                    dayMargin,
                    dayOrders.size()
            ));
        }

        report.setTotalRevenue(totalRevenue);
        report.setTotalCost(totalCost);
        report.setGrossProfit(totalRevenue.subtract(totalCost));
        report.setTotalItemsSold(totalItemsSold);
        report.setDailyBreakdown(dailyBreakdown);

        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal margin = report.getGrossProfit()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalRevenue, 2, RoundingMode.HALF_UP);
            report.setProfitMargin(margin);
        } else {
            report.setProfitMargin(BigDecimal.ZERO);
        }

        return report;
    }

    public RevenueReportDTO getTodayRevenue() {
        LocalDate today = LocalDate.now();
        return getRevenueReport(today, today, "daily");
    }

    public RevenueReportDTO getWeeklyRevenue() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        return getRevenueReport(startOfWeek, today, "weekly");
    }

    public RevenueReportDTO getMonthlyRevenue() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        return getRevenueReport(startOfMonth, today, "monthly");
    }

    public DailyReportDTO getDailyReport(LocalDate date) {
        List<SalesClient.OrderSnapshot> orders = salesClient.getOrders(date, date);
        List<SalesClient.OrderSnapshot> paidOrders = orders.stream()
                .filter(o -> "PAID".equalsIgnoreCase(o.getStatus()))
                .collect(Collectors.toList());

        List<SalesClient.OrderSnapshot> paidSales = paidOrders.stream()
                .filter(o -> !Boolean.TRUE.equals(o.getReturnOrder()))
                .collect(Collectors.toList());

        List<SalesClient.OrderSnapshot> paidReturns = paidOrders.stream()
                .filter(o -> Boolean.TRUE.equals(o.getReturnOrder()))
                .collect(Collectors.toList());

        BigDecimal salesRevenue = sumOrderAmount(paidSales);
        BigDecimal returnAmount = sumOrderAmount(paidReturns);

        long salesQuantity = sumOrderQuantity(paidSales);
        long returnQuantity = sumOrderQuantity(paidReturns);

        BigDecimal debtIncrease = orders.stream()
                .filter(o -> "UNPAID".equalsIgnoreCase(o.getStatus()))
                .filter(o -> !Boolean.TRUE.equals(o.getReturnOrder()))
                .map(o -> safeAmount(o.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal debtDecrease = BigDecimal.ZERO;

        Map<String, BigDecimal> incomeByMethod = new HashMap<>();
        Map<String, BigDecimal> expenseByMethod = new HashMap<>();

        List<SalesClient.PaymentSnapshot> payments = salesClient.getPayments(date, date);
        for (SalesClient.PaymentSnapshot payment : payments) {
            if (payment == null || payment.getAmount() == null) {
                continue;
            }
            String method = normalizePaymentMethod(payment.getMethod());
            incomeByMethod.merge(method, payment.getAmount(), BigDecimal::add);
        }

        for (SalesClient.OrderSnapshot order : paidReturns) {
            BigDecimal amount = safeAmount(order.getTotalAmount());
            String refundMethod = normalizePaymentMethod(order.getRefundMethod());
            expenseByMethod.merge(refundMethod, amount, BigDecimal::add);
        }

        DailyReportDTO report = new DailyReportDTO();
        report.setDate(date.format(DateTimeFormatter.ISO_DATE));
        report.setStartTime(date.atStartOfDay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        report.setEndTime(date.atTime(23, 59).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        DailyReportDTO.Breakdown income = new DailyReportDTO.Breakdown();
        income.setCash(getAmount(incomeByMethod, "CASH"));
        income.setCard(getAmount(incomeByMethod, "CARD"));
        income.setTransfer(getAmount(incomeByMethod, "TRANSFER"));
        income.setVoucher(getAmount(incomeByMethod, "VOUCHER"));
        income.setOther(getAmount(incomeByMethod, "OTHER"));
        income.setTotal(income.getCash()
                .add(income.getCard())
                .add(income.getTransfer())
                .add(income.getVoucher())
                .add(income.getOther()));

        DailyReportDTO.Breakdown expense = new DailyReportDTO.Breakdown();
        expense.setCash(getAmount(expenseByMethod, "CASH"));
        expense.setCard(getAmount(expenseByMethod, "CARD"));
        expense.setTransfer(getAmount(expenseByMethod, "TRANSFER"));
        expense.setVoucher(getAmount(expenseByMethod, "VOUCHER"));
        expense.setOther(getAmount(expenseByMethod, "OTHER"));
        expense.setTotal(expense.getCash()
                .add(expense.getCard())
                .add(expense.getTransfer())
                .add(expense.getVoucher())
                .add(expense.getOther()));

        DailyReportDTO.DebtSummary debt = new DailyReportDTO.DebtSummary();
        debt.setIncrease(debtIncrease);
        debt.setDecrease(debtDecrease);
        debt.setTotal(debtIncrease.subtract(debtDecrease));

        DailyReportDTO.Summary summary = new DailyReportDTO.Summary();
        summary.setIncome(income);
        summary.setExpense(expense);
        summary.setDebt(debt);
        summary.setNetTotal(income.getTotal().subtract(expense.getTotal()));
        report.setSummary(summary);

        DailyReportDTO.SalesSummary sales = new DailyReportDTO.SalesSummary();
        sales.setQuantity(salesQuantity);
        sales.setValue(salesRevenue);
        report.setSales(sales);

        DailyReportDTO.SalesSummary returns = new DailyReportDTO.SalesSummary();
        returns.setQuantity(returnQuantity);
        returns.setValue(returnAmount);
        report.setReturns(returns);

        DailyReportDTO.OtherSummary other = new DailyReportDTO.OtherSummary();
        other.setTotalInvoices(paidOrders.size());
        other.setSalesInvoices(paidSales.size());
        other.setReturnInvoices(paidReturns.size());
        other.setExchangeInvoices(paidReturns.stream()
                .filter(o -> isExchangeOrder(o.getOrderType()))
                .count());
        other.setVoucherCount(payments.stream()
                .filter(p -> "VOUCHER".equals(normalizePaymentMethod(p.getMethod())))
                .count());
        other.setDiscountCount(0);
        other.setCardPaymentCount(payments.stream()
                .filter(p -> "CARD".equals(normalizePaymentMethod(p.getMethod())))
                .count());
        report.setOther(other);

        DailyReportDTO.CashSummary cash = new DailyReportDTO.CashSummary();
        cash.setOpeningCash(BigDecimal.ZERO);
        cash.setCashCollected(income.getCash());
        cash.setCashRefunded(expense.getCash());
        cash.setCashNet(income.getCash().subtract(expense.getCash()));
        cash.setExpectedHandover(cash.getOpeningCash().add(cash.getCashNet()));
        report.setCash(cash);

        return report;
    }

    public Map<String, Object> getRevenueComparison() {
        Map<String, Object> comparison = new HashMap<>();

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        RevenueReportDTO todayReport = getRevenueReport(today, today, "daily");
        RevenueReportDTO yesterdayReport = getRevenueReport(yesterday, yesterday, "daily");

        comparison.put("todayRevenue", todayReport.getTotalRevenue());
        comparison.put("todayOrders", todayReport.getTotalOrders());
        comparison.put("todayProfit", todayReport.getGrossProfit());
        comparison.put("todayMargin", todayReport.getProfitMargin());
        comparison.put("todayChange", calculatePercentChange(
                yesterdayReport.getTotalRevenue(), todayReport.getTotalRevenue()));

        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endOfLastWeek = startOfWeek.minusDays(1);
        LocalDate startOfLastWeek = endOfLastWeek.minusDays(6);

        RevenueReportDTO thisWeekReport = getRevenueReport(startOfWeek, today, "weekly");
        RevenueReportDTO lastWeekReport = getRevenueReport(startOfLastWeek, endOfLastWeek, "weekly");

        comparison.put("weeklyRevenue", thisWeekReport.getTotalRevenue());
        comparison.put("weeklyOrders", thisWeekReport.getTotalOrders());
        comparison.put("weeklyProfit", thisWeekReport.getGrossProfit());
        comparison.put("weeklyMargin", thisWeekReport.getProfitMargin());
        comparison.put("weeklyChange", calculatePercentChange(
                lastWeekReport.getTotalRevenue(), thisWeekReport.getTotalRevenue()));

        LocalDate startOfMonth = today.withDayOfMonth(1);
        int daysInCurrentPeriod = today.getDayOfMonth();
        LocalDate startOfLastMonth = startOfMonth.minusMonths(1);
        LocalDate endOfLastMonthPeriod = startOfLastMonth.plusDays(daysInCurrentPeriod - 1);

        RevenueReportDTO thisMonthReport = getRevenueReport(startOfMonth, today, "monthly");
        RevenueReportDTO lastMonthReport = getRevenueReport(startOfLastMonth, endOfLastMonthPeriod, "monthly");

        comparison.put("monthlyRevenue", thisMonthReport.getTotalRevenue());
        comparison.put("monthlyOrders", thisMonthReport.getTotalOrders());
        comparison.put("monthlyProfit", thisMonthReport.getGrossProfit());
        comparison.put("monthlyMargin", thisMonthReport.getProfitMargin());
        comparison.put("monthlyChange", calculatePercentChange(
                lastMonthReport.getTotalRevenue(), thisMonthReport.getTotalRevenue()));

        comparison.put("marginChange", calculateMarginChange(
                lastMonthReport.getProfitMargin(), thisMonthReport.getProfitMargin()));

        return comparison;
    }

    private BigDecimal calculatePercentChange(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            if (current != null && current.compareTo(BigDecimal.ZERO) > 0) {
                return BigDecimal.valueOf(100);
            }
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMarginChange(BigDecimal previousMargin, BigDecimal currentMargin) {
        if (previousMargin == null) previousMargin = BigDecimal.ZERO;
        if (currentMargin == null) currentMargin = BigDecimal.ZERO;
        return currentMargin.subtract(previousMargin).setScale(1, RoundingMode.HALF_UP);
    }

    public List<TopProductDTO> getTopSellingProducts(LocalDate startDate, LocalDate endDate, int limit) {
        List<SalesClient.OrderSnapshot> orders = salesClient.getOrders(startDate, endDate);
        List<SalesClient.OrderSnapshot> paidOrders = orders.stream()
                .filter(o -> "PAID".equalsIgnoreCase(o.getStatus()))
                .filter(o -> !Boolean.TRUE.equals(o.getReturnOrder()))
                .collect(Collectors.toList());

        Map<Long, ProductSalesData> productSales = new HashMap<>();

        for (SalesClient.OrderSnapshot order : paidOrders) {
            if (order.getItems() == null) {
                continue;
            }
            for (SalesClient.OrderItemSnapshot item : order.getItems()) {
                if (item.getProductId() == null) {
                    continue;
                }
                ProductSalesData data = productSales.getOrDefault(item.getProductId(), new ProductSalesData());
                Integer qtyValue = item.getQuantity();
                int qty = qtyValue != null ? qtyValue : 0;
                BigDecimal itemRevenue = safeAmount(item.getPrice()).multiply(BigDecimal.valueOf(qty));
                data.quantitySold += qty;
                data.totalRevenue = data.totalRevenue.add(itemRevenue);
                productSales.put(item.getProductId(), data);
            }
        }

        Map<Long, InventoryClient.StockSummary> stockMap = inventoryClient.getAllStocks().stream()
                .filter(s -> s.getProductId() != null)
                .collect(Collectors.toMap(InventoryClient.StockSummary::getProductId, s -> s, (a, b) -> a));

        List<TopProductDTO> results = new ArrayList<>();
        for (Map.Entry<Long, ProductSalesData> entry : productSales.entrySet()) {
            Long productId = entry.getKey();
            ProductSalesData data = entry.getValue();
            CatalogClient.ProductSnapshot product = catalogClient.getProduct(productId);

            BigDecimal costPrice = getCostPrice(productId);
            BigDecimal totalCost = costPrice.multiply(BigDecimal.valueOf(data.quantitySold));
            BigDecimal profit = data.totalRevenue.subtract(totalCost);

            InventoryClient.StockSummary stock = stockMap.get(productId);

            TopProductDTO dto = new TopProductDTO(
                    productId,
                    product == null ? null : product.getName(),
                    product == null ? null : product.getCode(),
                    product == null ? null : product.getCategoryId(),
                    data.quantitySold,
                    data.totalRevenue,
                    totalCost,
                    profit,
                    stock == null ? null : stock.getStock()
            );
            results.add(dto);
        }

        return results.stream()
                .sorted((a, b) -> Long.compare(b.getQuantitySold(), a.getQuantitySold()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<LowStockAlertDTO> getLowStockAlerts(Integer threshold) {
        int limit = threshold == null ? DEFAULT_LOW_STOCK_THRESHOLD : threshold;
        List<InventoryClient.StockSummary> stocks = inventoryClient.getAllStocks();
        List<LowStockAlertDTO> result = new ArrayList<>();
        for (InventoryClient.StockSummary stock : stocks) {
            Integer currentValue = stock.getStock();
            int current = currentValue != null ? currentValue : 0;
            if (current > limit) {
                continue;
            }
            String level = current <= CRITICAL_STOCK_THRESHOLD ? "CRITICAL" : "WARNING";
            LowStockAlertDTO alert = new LowStockAlertDTO(
                    stock.getProductId(),
                    stock.getProductName(),
                    stock.getProductCode(),
                    stock.getCategoryId(),
                    current,
                    limit,
                    level
            );
            result.add(alert);
        }
        return result;
    }

    public Map<String, Long> getLowStockSummary(Integer threshold) {
        int limit = threshold == null ? DEFAULT_LOW_STOCK_THRESHOLD : threshold;
        long critical = 0;
        long warning = 0;
        long total = 0;
        for (LowStockAlertDTO alert : getLowStockAlerts(limit)) {
            total++;
            if ("CRITICAL".equalsIgnoreCase(alert.getAlertLevel())) {
                critical++;
            } else {
                warning++;
            }
        }
        Map<String, Long> summary = new HashMap<>();
        summary.put("total", total);
        summary.put("critical", critical);
        summary.put("warning", warning);
        return summary;
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal sumOrderAmount(List<SalesClient.OrderSnapshot> orders) {
        return orders.stream()
                .map(o -> safeAmount(o.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long sumOrderQuantity(List<SalesClient.OrderSnapshot> orders) {
        long total = 0;
        for (SalesClient.OrderSnapshot order : orders) {
            if (order.getItems() == null) {
                continue;
            }
            for (SalesClient.OrderItemSnapshot item : order.getItems()) {
                Integer qtyValue = item.getQuantity();
                total += qtyValue != null ? qtyValue : 0;
            }
        }
        return total;
    }

    private BigDecimal getAmount(Map<String, BigDecimal> map, String key) {
        return map.getOrDefault(key, BigDecimal.ZERO);
    }

    private String normalizePaymentMethod(String method) {
        if (method == null || method.isBlank()) {
            return "OTHER";
        }
        String normalized = method.trim().toUpperCase();
        return switch (normalized) {
            case "CASH", "CARD", "TRANSFER", "VOUCHER" -> normalized;
            default -> "OTHER";
        };
    }

    private boolean isExchangeOrder(String orderType) {
        if (orderType == null) {
            return false;
        }
        String normalized = orderType.trim().toUpperCase();
        return "EXCHANGE".equals(normalized);
    }

    private static class ProductSalesData {
        long quantitySold = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
    }
}

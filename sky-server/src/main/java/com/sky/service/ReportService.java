package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

/**
 * @author wyr on 2025/6/25
 */

public interface ReportService {
    /** * 营业额统计
     * 统计指定时间段内的营业额
     * @param begin
     * @param end
     * @return
     */
    TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end);

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    UserReportVO userStatistics(LocalDate begin, LocalDate end);
    /**
     * 订单统计
     * @param begin 开始日期
     * @param end 结束日期
     * @return 订单统计结果视图对象
     */
    OrderReportVO orderStatistics(LocalDate begin, LocalDate end);
    /**
     * 销售额前10商品统计
     * @param begin 开始日期
     * @param end 结束日期
     * @return 销售额前10商品统计结果视图对象
     */
    SalesTop10ReportVO salesTop10Statistics(LocalDate begin, LocalDate end);

    /**
     * 导出营业数据
     * @param response
     */
    void exportBusinessData(HttpServletResponse response);
}


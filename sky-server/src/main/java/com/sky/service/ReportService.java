package com.sky.service;

import com.sky.vo.TurnoverReportVO;

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
}


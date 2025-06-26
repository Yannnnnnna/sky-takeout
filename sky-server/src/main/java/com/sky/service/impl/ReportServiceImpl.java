package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author wyr on 2025/6/25
 */
@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper; // 假设有一个订单数据访问层
    @Autowired
    private UserMapper userMapper; // 假设有一个用户数据访问层
    @Autowired
    private WorkspaceServiceImpl workspaceService; // 假设有一个工作区服务层

    /**
     * 营业额统计
     * 统计指定时间段内的营业额
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 营业额统计结果
     */
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        //存放每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.isEqual(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Double> turnoverList = new ArrayList<>();
        //遍历日期集合，获取营业额集合
        for (LocalDate date : dateList) {
            LocalDateTime dayBegin = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", dayBegin);
            map.put("end", dayEnd);
            map.put("status", Orders.COMPLETED);
            Double count = Double.valueOf(orderMapper.countByMap(map));
            turnoverList.add(count == null ? 0.0 : count);

        }
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 用户统计
     * 统计指定时间段内的用户数据
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        //存放每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.isEqual(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> userCountList = new ArrayList<>();
        List<Integer> newUserCountList = new ArrayList<>();
        for (LocalDate localDate : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);
            Map map = new HashMap();
            map.put("end", endTime);
            Integer userCount = userMapper.countByMap(map);
            userCountList.add(userCount == null ? 0 : userCount);

            map.put("begin", beginTime);
            Integer newUserCount = userMapper.countByMap(map);
            newUserCountList.add(newUserCount == null ? 0 : newUserCount);

        }
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(userCountList, ","))
                .newUserList(StringUtils.join(newUserCountList, ","))
                .build();
    }

    /**
     * 订单统计
     * 统计指定时间段内的订单数据
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 订单统计结果
     */
    @Override
    public OrderReportVO orderStatistics(LocalDate begin, LocalDate end) {
        //存放每天的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.isEqual(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        Integer totalOrderCount = 0;
        Integer validOrderCount = 0;
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate localDate : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);
            Map<String, Object> map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            Integer orderCount = orderMapper.getCountByMap(map);
            map.put("status", Orders.COMPLETED);
            Integer validCount = orderMapper.getCountByMap(map);
            orderCountList.add(orderCount == null ? 0 : orderCount);
            validOrderCountList.add(validCount == null ? 0 : validCount);
            totalOrderCount += orderCount == null ? 0 : orderCount;
            validOrderCount += validCount == null ? 0 : validCount;
        }
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount ;

        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }
    /**
     * 销售额前10商品统计
     * 统计指定时间段内销售额前10的商品
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 销售额前10商品统计结果
     */
    @Override
    public SalesTop10ReportVO salesTop10Statistics(LocalDate begin, LocalDate end) {

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime,endTime);

        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String nameLists = StringUtils.join(names, ",");
        String numberLists = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameLists)
                .numberList(numberLists)
                .build();
    }

    /**
     * 导出营业数据
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //1. 查询数据库，获取营业数据
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        //1.1 查询概览数据
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));
        //2. 通过poi将数据写入Excel表格中
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            XSSFWorkbook excel = new XSSFWorkbook(inputStream);
            //2.1 获取工作表
            XSSFSheet sheet = excel.getSheet("Sheet1");
            //2.2 填充数据
            sheet.getRow(1).getCell(1).setCellValue("时间：" + begin + "至" + end);
            //获得第4行
            XSSFRow row = sheet.getRow(3);
            //获取单元格
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                //准备明细数据
                businessData = workspaceService.getBusinessData(LocalDateTime.of(date,LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }
            //通过输出流将文件下载到客户端浏览器中
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            //关闭资源
            out.flush();
            out.close();
            excel.close();

        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}

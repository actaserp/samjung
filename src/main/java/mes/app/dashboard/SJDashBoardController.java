package mes.app.dashboard;

import mes.app.dashboard.service.DashBoardService;
import mes.app.dashboard.service.SJDashBoardService;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/sjdashboard")
public class SJDashBoardController {

    @Autowired
    private SJDashBoardService dashBoardService;

    @GetMapping("/read")
    private AjaxResult getList(@RequestParam(value = "startDate", required = false) String startDate,
                               @RequestParam(value = "endDate", required = false) String endDate,
                               @RequestParam(value = "search_type", required = false) String searchType,
                               Authentication auth) {

        String search_startDate = (startDate).replaceAll("-", "");
        String search_endDate = (endDate).replaceAll("-", "");

        List<Map<String, Object>> items = this.dashBoardService.getList(search_startDate, search_endDate, searchType);

        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }

    @GetMapping("/read_month")
    private AjaxResult getListMonth(@RequestParam(value = "startDate") String startDate,
                                    @RequestParam(value = "endDate") String endDate,
                                    Authentication auth) {

        String search_startDate = startDate.replaceAll("-", "");
        String search_endDate = endDate.replaceAll("-", "");

        List<Map<String, Object>> items = this.dashBoardService.getList(search_startDate, search_endDate, null);

        // 날짜별 ordflag 그룹 정리
        Map<String, Map<String, Integer>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> item : items) {
            String date = String.valueOf(item.get("reqdate")).substring(0, 8);
            String ordflag = String.valueOf(item.get("ordflag"));

            String category = switch (ordflag) {
                case "발주처-출고완료", "발주처-부분출고" -> "발주출고";
                case "공장-출고완료", "공장-부분출고" -> "공장출고";
                case "현장-확인완료", "현장-부분확인" -> "현장확인";
                case "발주등록" -> "발주등록";
                case "프로젝트 등록" -> "프로젝트 등록";
                default -> null;
            };

            if (category != null) {
                grouped.computeIfAbsent(date, k -> new HashMap<>());
                Map<String, Integer> dateGroup = grouped.get(date);
                dateGroup.put(category, dateGroup.getOrDefault(category, 0) + 1);
            }
        }

        // 캘린더용 배열로 변환
        List<Map<String, Object>> calendarEvents = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> entry : grouped.entrySet()) {
            String date = entry.getKey();
            Map<String, Integer> types = entry.getValue();
            for (Map.Entry<String, Integer> t : types.entrySet()) {
                Map<String, Object> ev = new HashMap<>();
                ev.put("title", t.getKey() + "(" + t.getValue() + ")");
                ev.put("start", date); // yyyyMMdd
                calendarEvents.add(ev);
            }
        }

        AjaxResult result = new AjaxResult();
        result.data = calendarEvents;
        return result;
    }

    @GetMapping("/yearCount")
    private AjaxResult getYearCount(@RequestParam(value = "year") String year, Authentication auth) {
        List<Map<String, Object>> items = dashBoardService.getYearCountList(year);
        AjaxResult result = new AjaxResult();

        result.data = items;
        return result;
    }



}

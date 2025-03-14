package mes.app.dashboard;

import mes.app.dashboard.service.DashBoardService2;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard2")
public class DashBoardController2 {
    @Autowired
    private DashBoardService2 dashBoardService2;

    // 작년 1월1일부터 작년오늘날자까지 상태별 건수
    @GetMapping("/LastYearCnt")
    private AjaxResult LastYearCnt(@RequestParam(value = "search_spjangcd") String search_spjangcd
                                    , Authentication auth) {
        // 관리자 사용가능 페이지 사업장 코드 선택 로직
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        String spjangcd = dashBoardService2.getSpjangcd(username, search_spjangcd);
        // 사업장 코드 선택 로직 종료 반환 spjangcd 활용

        // 작년 진행구분(ordflag)별 데이터 개수
        List<Map<String, Object>> LastYearCnt = this.dashBoardService2.LastYearCnt(spjangcd);
        // 올해 진행구분(ordflag)별 데이터 개수
        List<Map<String, Object>> ThisYearCnt = this.dashBoardService2.ThisYearCnt(spjangcd);

        // 그래프 필요 데이터 (일별 - 이번달 일별, 작년 동월 일별)
        List<Map<String, Object>> ThisMonthCntOfOrdflag = this.dashBoardService2.ThisMonthCntOfOrdflag(spjangcd);
        List<Map<String, Object>> LastYearCntOfDate = this.dashBoardService2.LastYearCntOfDate(spjangcd);
        //                  (월별 - 올해 월별, 작년 월별)
        List<Map<String, Object>> ThisYearCntOfOrdflagByMonth = this.dashBoardService2.ThisYearCntOfOrdflagByMonth(spjangcd);
        List<Map<String, Object>> LastYearCntOfDateByMonth = this.dashBoardService2.LastYearCntOfDateByMonth(spjangcd);
        //                  (분기별 - 올해 분기별, 작년 분기별)
        List<Map<String, Object>> ThisYearCntOfOrdflagByQuarter = this.dashBoardService2.ThisYearCntOfOrdflagByQuarter(spjangcd);
        List<Map<String, Object>> LastYearCntOfDateByQuarter = this.dashBoardService2.LastYearCntOfDateByQuarter(spjangcd);
        //                  (년별 - 서비스 시작후 년별)
        List<Map<String, Object>> LastFiveYearsCntOfOrdflag = this.dashBoardService2.LastFiveYearsCntOfOrdflag(spjangcd);
        //                  (전년대비(월별) - 올해 월별, 작년 월별)(월별 데이터 활용)
        //                  (YTD - 올해 월별(stacked), 작년 월별(stacked))
        List<Map<String, Object>> ThisYearCntOfStacked = this.dashBoardService2.ThisYearCntOfStacked(spjangcd);
        List<Map<String, Object>> LastYearCntOfStacked = this.dashBoardService2.LastYearCntOfStacked(spjangcd);



        AjaxResult result = new AjaxResult();
        Map<String, Object> items = new HashMap<String, Object>();
        items.put("LastYearCnt", LastYearCnt);
        items.put("ThisYearCnt", ThisYearCnt);
        items.put("ThisMonthCntOfOrdflag", ThisMonthCntOfOrdflag);
        items.put("LastYearCntOfDate", LastYearCntOfDate);
        items.put("ThisYearCntOfOrdflagByMonth", ThisYearCntOfOrdflagByMonth);
        items.put("LastYearCntOfDateByMonth", LastYearCntOfDateByMonth);
        items.put("ThisYearCntOfOrdflagByQuarter", ThisYearCntOfOrdflagByQuarter);
        items.put("LastYearCntOfDateByQuarter", LastYearCntOfDateByQuarter);
        items.put("LastFiveYearsCntOfOrdflag", LastFiveYearsCntOfOrdflag);
        items.put("ThisYearCntOfStacked", ThisYearCntOfStacked);
        items.put("LastYearCntOfStacked", LastYearCntOfStacked);

        result.data = items;

        return result;
    }
    @GetMapping("/bindSpjangcd")
    public AjaxResult bindSpjangcd(Authentication auth) {
        // 관리자 사용가능 페이지 사업장 코드 선택 로직
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        String spjangcd = dashBoardService2.getSpjangcd(username, "");
        // 사업장 코드 선택 로직 종료 반환 spjangcd 활용
        AjaxResult result = new AjaxResult();
        result.data = spjangcd;
        return result;
    }

}

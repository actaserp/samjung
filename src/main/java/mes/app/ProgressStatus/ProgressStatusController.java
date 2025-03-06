package mes.app.ProgressStatus;


import lombok.extern.slf4j.Slf4j;
import mes.app.ProgressStatus.service.ProgressStatusService;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/ProgressStatus")
public class ProgressStatusController {

    @Autowired
    ProgressStatusService progressStatusService;


    @GetMapping("/read")
    public AjaxResult ProgressStatusRead(Authentication auth,
                                         @RequestParam(value = "search_spjangcd", required = false) String search_spjangcd,
                                         @RequestParam(value = "startDate", required = false) String startDate,
                                         @RequestParam(value = "endDate", required = false) String endDate) {
        AjaxResult result = new AjaxResult();

        try{
            // 로그인한 사용자 정보에서 이름(perid) 가져오기
            User user = (User) auth.getPrincipal();
            String perid = user.getFirst_name();

            List<Map<String, Object>> progressStatusLis = progressStatusService.getProgressStatusList(perid, search_spjangcd, startDate, endDate);

            result.success = true;
            result.data = progressStatusLis;
            result.message = "데이터 조회 성공";

        } catch (Exception e) {
            // 오류 발생 시 실패 상태 설정
            result.success = false;
            result.message = "데이터를 가져오는 중 오류가 발생했습니다.";
        }

        return result;
    }

    @GetMapping("/search")
    public AjaxResult searchProgress(@RequestParam Map<String, String> params, Authentication auth,
                                     @RequestParam(value = "search_spjangcd", required = false) String search_spjangcd) {
        AjaxResult result = new AjaxResult();
        log.info("그리드 검색: {}", params);
        try {
            // 로그인한 사용자 정보
            User user = (User) auth.getPrincipal();
            String userid = user.getUsername();

            // 검색 조건 로깅
            //System.out.println(String.format("검색 조건: %s, 사용자: %s, 사업장: %s", params, userid, search_spjangcd));

            // groupByColumns 처리
            List<String> groupByColumns = null;
            if (params.containsKey("groupByColumns") && !params.get("groupByColumns").isEmpty()) {
                groupByColumns = Arrays.asList(params.get("groupByColumns").split(",")); // 쉼표로 구분
            }

            // 검색 서비스 호출
            List<Map<String, Object>> progressStatusList = progressStatusService.searchProgressStatus(
                    params.get("startDate"),    // 검색 시작 날짜
                    params.get("endDate"),      // 검색 종료 날짜
                    params.get("searchTitle"), // 검색 비고
                    params.get("searchtketnm"),
                    params.get("searchCltnm"),
                    userid,                     // 사용자 ID
                    search_spjangcd,                   // 사업장 코드
                    groupByColumns              // 그룹바이 조건
            );

            // 응답 생성
            result.success = true;
            result.data = progressStatusList;
            result.message = "데이터 조회 성공";
        } catch (Exception e) {
            // 예외 로그 출력
            log.error("검색 중 오류 발생: {}", e.getMessage(), e);

            // 응답 생성
            result.success = false;
            result.message = "데이터를 가져오는 중 오류가 발생했습니다.";
        }

        return result;
    }

    //차트 데이터
    @GetMapping("/getChartData2")
    public AjaxResult getChartData(
            Authentication auth,
            @RequestParam(value = "search_spjangcd", required = false) String search_spjangcd,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "searchCltnm", required = false) String searchCltnm,
            @RequestParam(value = "searchtketnm", required = false) String searchtketnm,
            @RequestParam(value = "searchTitle", required = false) String searchTitle) {

        AjaxResult result = new AjaxResult();
        try {
            User user = (User) auth.getPrincipal();
            String userid = user.getUsername();

            /*log.info("검색 조건 search_spjangcd:{}, startDate: {}, endDate: {}, cltnm: {}, ordflag: {}, searchTitle: {}",
                    search_spjangcd, startDate, endDate, searchCltnm, searchtketnm, searchTitle);*/

            List<Map<String, Object>> rawData = progressStatusService.getChartData2(
                    userid, search_spjangcd, startDate, endDate, searchCltnm, searchtketnm, searchTitle);

            if (rawData == null || rawData.isEmpty()) {
                log.warn("조회된 데이터가 없습니다.");
                result.success = false;
                result.message = "조회된 데이터가 없습니다.";
            } else {
                //log.info("쿼리 결과 데이터: {}", rawData);
                result.success = true;
                result.data = rawData;
            }
        } catch (Exception e) {
            log.error("데이터 처리 중 오류: {}", e.getMessage(), e);
            result.success = false;
            result.message = "데이터를 가져오는 중 오류가 발생했습니다.";
        }

        return result;
    }


}

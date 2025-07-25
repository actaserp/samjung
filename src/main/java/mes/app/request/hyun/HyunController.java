package mes.app.request.hyun;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mes.app.request.hyun.service.HyunService;
import mes.app.request.subRequest.service.SubRequestService;
import mes.config.Settings;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.repository.actasRepository.TB_DA006WFILERepository;
import mes.domain.repository.actasRepository.TB_DA006WRepository;
import mes.domain.repository.actasRepository.TB_DA007WRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/request/hyun")
public class HyunController {
    @Autowired
    private HyunService requestService;


    @Autowired
    private TB_DA007WRepository tbda007WRepository;

    @Autowired
    private TB_DA006WRepository tbda006WRepository;

    @Autowired
    private TB_DA006WFILERepository tbDa006WFILERepository;

    @Autowired
    Settings settings;
    // 주문출고(현장) 그리드 read
    @GetMapping("/order_read")
    public AjaxResult getOrderList2(@RequestParam(value = "search_startDate", required = false) String searchStartDate,
                                    @RequestParam(value = "search_endDate", required = false) String searchEndDate,
                                    @RequestParam(value = "searchText", required = false) String searchActnm,
                                    Authentication auth) {
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        String saupnum;
        String cltcd;
        Map<String, Object> userInfo = requestService.getUserInfo(username);

        String search_startDate = (searchStartDate).replaceAll("-","");
        String search_endDate = (searchEndDate).replaceAll("-","");
        List<Map<String, Object>> items = this.requestService.getOrderList2(
                search_startDate, search_endDate, searchActnm);
//        Map<String, Object> headitem = this.requestService.getHeadList(tbDa006WPk);
//        items.add(headitem);
        if(items != null){
            for (Map<String, Object> item : items) {
                // 날짜 형식 변환 (FACDATE)
                if (item.containsKey("HYUNDATE")) {
                    String setupdt = (String) item.get("HYUNDATE");
                    if (setupdt != null && setupdt.length() == 8) {
                        String formattedDate = setupdt.substring(0, 4) + "-" + setupdt.substring(4, 6) + "-" + setupdt.substring(6, 8);
                        item.put("HYUNDATE", formattedDate);
                    }
                }
            }
        }

        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }
    // 주문출고(현장) flag값 update
    @PostMapping("/saveDelivery")
    public AjaxResult saveDelivery(@RequestParam Map<String, Object> param,
                                   Authentication auth) {
        AjaxResult result = new AjaxResult();
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // sendData를 List<Map<String, Object>>로 안전하게 변환
        ObjectMapper mapper = new ObjectMapper();
        String sendDataStr = (String) param.get("sendData");
        List<Map<String, Object>> sendData = new ArrayList<>();
        if (sendDataStr != null && !sendDataStr.isEmpty() && !"null".equals(sendDataStr)) {
            try {
                sendData = mapper.readValue(sendDataStr,
                        new TypeReference<List<Map<String, Object>>>() {});
            } catch (JsonProcessingException e) {
                // 에러시 값, 에러 메시지 출력
                System.out.println("파싱에러: " + e.getMessage());
                sendData = new ArrayList<>(); // 혹은 적절히 처리
            }
        }
        try {
            for (Map<String, Object> item : sendData) {
                String chulFlag = normalizeChulFlag(item.get("FACFLAG"));
                Integer baljuNum = toInt(item.get("BALJUNUM"));
                Integer baljuSeq = toInt(item.get("BALJUSEQ"));

                // 1. 기존값 조회 (null이면 0 취급, 필요시 변환)
                String oldFlag = (String) requestService.getFacFlag(baljuNum, baljuSeq).get("FACFLAG");
                if (oldFlag == null) oldFlag = "0";

                // 2. 값이 다를 때만 처리
                if (!Objects.equals(oldFlag, chulFlag)) {
                    if ("1".equals(chulFlag)) { // 출고처리
                        // 0 → 1
                        requestService.updateFacInfo2(username, today, chulFlag, baljuNum, baljuSeq);
                    } else { // 출고 해제
                        // 1 → 0
                        requestService.clearChulInfo2("0", baljuNum, baljuSeq);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            result.success = true;
        }
        return result;
    }

    private Integer toInt(Object value) {
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value != null) return Integer.parseInt(value.toString());
        return null;
    }
    private String normalizeChulFlag(Object flag) {
        if (flag == null) return "0";
        if (flag instanceof Boolean) return (Boolean) flag ? "1" : "0";
        if (flag instanceof Number) return ((Number) flag).intValue() == 1 ? "1" : "0";
        String s = flag.toString().trim();
        if ("1".equals(s) || "true".equalsIgnoreCase(s)) return "1";
        return "0";
    }


    // "yyyyMMdd" -> "yyyy년 MM월 dd일"
    public static String formatToKorean(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() != 8) return "";
        try {
            DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyyMMdd");
            DateTimeFormatter kor = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
            LocalDate date = LocalDate.parse(yyyymmdd, ymd);
            return date.format(kor);
        } catch (Exception e) {
            return "";
        }
    }
    // "yyyyMMdd" -> "yyyy-MM-dd"
    public static String formatToDash(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() != 8) return "";
        try {
            DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyyMMdd");
            DateTimeFormatter dash = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate date = LocalDate.parse(yyyymmdd, ymd);
            return date.format(dash);
        } catch (Exception e) {
            return "";
        }
    }
    // 금액 콤마 메서드
    public static String formatPrice(Object value) {
        if (value == null) return "";
        try {
            long val = value instanceof Number ? ((Number) value).longValue() : Long.parseLong(value.toString());
            return String.format("%,d", val);
        } catch(Exception e) { return value.toString(); }
    }


}


package mes.app.request.subRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mes.app.request.subRequest.service.SubRequestService;
import mes.config.Settings;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.repository.actasRepository.TB_DA006WFILERepository;
import mes.domain.repository.actasRepository.TB_DA006WRepository;
import mes.domain.repository.actasRepository.TB_DA007WRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/request/subRequest")
public class SubRequestController {
    @Autowired
    private SubRequestService requestService;


    @Autowired
    private TB_DA007WRepository tbda007WRepository;

    @Autowired
    private TB_DA006WRepository tbda006WRepository;

    @Autowired
    private TB_DA006WFILERepository tbDa006WFILERepository;

    @Autowired
    Settings settings;
    // 주문출고(공장) 그리드 read
    @GetMapping("/order_read")
    public AjaxResult getOrderList2(@RequestParam(value = "search_startDate", required = false) String searchStartDate,
                                    @RequestParam(value = "search_endDate", required = false) String searchEndDate,
                                    @RequestParam(value = "search_remark", required = false) String searchRemark,
                                    @RequestParam(value = "search_chulflag", required = false) String searchChulflag,
                                    @RequestParam(value = "saupnumHidden", required = false) String saupnumHidden,
                                    @RequestParam(value = "saupnum", required = false) String Saupnum,
                                    @RequestParam(value = "search_actnm", required = false) String searchActnm,
                                    @RequestParam(value = "spjangcd", required = false) String spjangcd,
                                    Authentication auth) {
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        String saupnum;
        String cltcd;
        Map<String, Object> userInfo = requestService.getUserInfo(username);

        // 관리자 진입시 사업자 번호 없이 업체명(searchActnm)만으로 조회
        if(userInfo == null) {
            saupnum = null;
            cltcd = null;
        } else{
            saupnum = (String) userInfo.get("saupnum");
            cltcd = (String) userInfo.get("cltcd");
        }
        String search_startDate = (searchStartDate).replaceAll("-","");
        String search_endDate = (searchEndDate).replaceAll("-","");
        List<Map<String, Object>> items = this.requestService.getOrderList2(spjangcd,
                search_startDate, search_endDate, searchRemark, searchChulflag, saupnum, cltcd, searchActnm);
//        Map<String, Object> headitem = this.requestService.getHeadList(tbDa006WPk);
//        items.add(headitem);
        if(items != null){
            for (Map<String, Object> item : items) {
                // 날짜 형식 변환 (reqdate)
                if (item.containsKey("CHULDATE")) {
                    String setupdt = (String) item.get("CHULDATE");
                    if (setupdt != null && setupdt.length() == 8) {
                        String formattedDate = setupdt.substring(0, 4) + "-" + setupdt.substring(4, 6) + "-" + setupdt.substring(6, 8);
                        item.put("CHULDATE", formattedDate);
                    }
                }
            }
        }

        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }
    // 주문출고(공장) flag값 update
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
                        requestService.updateChulInfo2(username, today, chulFlag, baljuNum, baljuSeq);
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

    // 엑셀파일 조회 및 파일 보기 메서드
    @GetMapping("/readVacFile")
    public void readVacFile(@RequestParam(value = "search_Date", required = false) String searchDate,
                            @RequestParam(value = "search_actnm", required = false) String searchActnm,
//                            @RequestParam(value = "spjangcd", required = false) String spjangcd,
                            HttpServletResponse response,
                            Authentication auth) throws Exception {
        Path tempXlsx = null;
        Path tempPdf = null;
        try {
            User user = (User) auth.getPrincipal();
            String username = user.getUsername();
            String spjangcd = "ZZ";

            Map<String, Object> userInfo = requestService.getUserInfo(username);
            String cltcd = userInfo != null ? (String) userInfo.get("cltcd") : null;

            // 주문출고 리스트 조회
            String search_Date = (searchDate).replaceAll("-", "");
            List<Map<String, Object>> vacData = requestService.getVacFileList(
                    search_Date,
                    searchActnm,
                    cltcd,
                    spjangcd
            );
            if (vacData == null || vacData.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"데이터가 없습니다\"}");
                response.flushBuffer();
                return;
            }
            Map<String, Object> head = vacData.get(0);
            // 1. UUID 기반 임시 파일명 생성
            String uuid = UUID.randomUUID().toString();
            tempXlsx = Files.createTempFile(uuid, ".xlsx");
            tempPdf = Path.of(tempXlsx.toString().replace(".xlsx", ".pdf"));
            // 2. 엑셀 템플릿 불러오기 및 수정(설치자재 출하 및 인수확인서)
            System.out.println(">>> PDF 응답 직전");
            try (FileInputStream fis = new FileInputStream("C:/Temp/mes21/문서/ReceiptConfirmation.xlsx");
                 Workbook workbook = new XSSFWorkbook(fis);
                 FileOutputStream fos = new FileOutputStream(tempXlsx.toFile())) {

                Sheet sheet = workbook.getSheetAt(0);
                // 660 정보 setCell
                // HEAD 정보
                setCell(sheet, 4, 2, (String) head.get("PROCD"));           // C5
                setCell(sheet, 5, 2, (String) head.get("ACTNM"));           // C6
                String chulDateStr = head.get("CHULDATE") == null ? "" : head.get("CHULDATE").toString();
                setCell(sheet, 4, 13, formatToKorean((String) head.get("CHULDATE"))); // N5

                String remarkAll =
                        (head.get("REMARK01") != null ? head.get("REMARK01") + "\n" : "") +
                                (head.get("REMARK02") != null ? head.get("REMARK02") + "\n" : "") +
                                (head.get("REMARK03") != null ? head.get("REMARK03") : "");
                setCell(sheet, 29, 5, remarkAll.trim()); // F30

                setCell(sheet, 34, 15, (String) head.get("last_name"));              // P35
                setCell(sheet, 34, 6, (String) head.get("cltnm"));               // G35
                setCell(sheet, 34, 12, (String) head.get("phone"));                 // M35
                setCell(sheet, 34, 3, formatToDash((String) head.get("CHULDATE")));   // D35 yyyy-mm-dd
                setCell(sheet, 36, 3, formatToDash((String) head.get("CHULDATE")));   // D37
                setCell(sheet, 38, 3, formatToDash((String) head.get("CHULDATE")));   // D39


                // BODY 정보 (표 9행~29행, 최대 21개 row)
                for (int i = 0; i < 21; i++) {
                    int row = 8 + i; // B9부터
                    if (i < vacData.size()) {
                        Map<String, Object> line = vacData.get(i);
                        setCell(sheet, row, 0, String.valueOf(i + 1));      // 번호, A9~A32
                        setCell(sheet, row, 1, (String) line.get("PNAME"));        // 품목, B9~B32
                        setCell(sheet, row, 5, (String) line.get("PSIZE"));        // 규격, F9~F32
                        setCell(sheet, row, 9, (String) line.get("PUNIT"));        // 단위, J9~J32
                        setCell(sheet, row, 11, line.get("PQTY").toString());        // 수량, L9~L32
                        setCell(sheet, row, 13, (String) line.get("CHULFLAG"));    // 출하여부, N9~N32
                        setCell(sheet, row, 14, (String) line.get("REMARK"));      // 비고, O9~O32
                    } else {
                        // 빈 row 초기화 (공백)
                        setCell(sheet, row, 0, "");
                        setCell(sheet, row, 1, "");
                        setCell(sheet, row, 5, "");
                        setCell(sheet, row, 9, "");
                        setCell(sheet, row, 11, "");
                        setCell(sheet, row, 13, "");
                        setCell(sheet, row, 14, "");
                    }
                }

                workbook.write(fos);
            }
            System.out.println(">>> PDF 응답 후");

            // 3. LibreOffice로 PDF 변환
            ProcessBuilder pb = new ProcessBuilder(
                    "C:/Program Files/LibreOffice/program/soffice.exe",
                    "--headless",
                    "--convert-to", "pdf",
                    "--outdir", tempPdf.getParent().toString(),
                    tempXlsx.toAbsolutePath().toString()
            );
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();

            // 4. PDF 파일이 실제로 존재하는지 체크
            if (!Files.exists(tempPdf) || Files.size(tempPdf) == 0) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"PDF 생성 실패\"}");
                response.flushBuffer();
                return;
            }

            // 5. PDF 응답 전송 (정상 동작)
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=vacation.pdf");
            try (FileInputStream fis = new FileInputStream(tempPdf.toFile())) {
                IOUtils.copy(fis, response.getOutputStream());
                response.flushBuffer();
            }
        } catch (Exception e) {
            // 예외 발생 시 명확한 메시지 반환
            System.out.println(">>> 예외 발생: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"서버 에러: " + e.getMessage() + "\"}");
            response.flushBuffer();
        } finally {
            // 6. 임시 파일 삭제 (즉시)
            final Path deleteXlsx = tempXlsx;
            final Path deletePdf = tempPdf;

            Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                try { if (deleteXlsx != null) Files.deleteIfExists(deleteXlsx); } catch (Exception ex) {}
                try { if (deletePdf != null) Files.deleteIfExists(deletePdf); } catch (Exception ex) {}
            }, 5, TimeUnit.MINUTES);
        }
    }
    // 엑셀파일 조회 및 파일 보기 메서드(거래명세서)
    @GetMapping("/readVacFile2")
    public void readVacFile2(@RequestParam(value = "search_Date", required = false) String searchDate,
                             @RequestParam(value = "search_actnm", required = false) String searchActnm,
//                            @RequestParam(value = "spjangcd", required = false) String spjangcd,
                             HttpServletResponse response,
                             Authentication auth) throws Exception {
        Path tempXlsx = null;
        Path tempPdf = null;
        try {
            User user = (User) auth.getPrincipal();
            String username = user.getUsername();
            String spjangcd = "ZZ";

            Map<String, Object> userInfo = requestService.getUserInfo(username);
            String cltcd = userInfo != null ? (String) userInfo.get("cltcd") : null;

            // 주문출고 리스트 조회
            String search_Date = (searchDate).replaceAll("-", "");
            List<Map<String, Object>> vacData = requestService.getVacFileList2(
                    search_Date,
                    searchActnm,
                    cltcd,
                    spjangcd
            );
            if (vacData == null || vacData.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"데이터가 없습니다\"}");
                response.flushBuffer();
                return;
            }
            Map<String, Object> head = vacData.get(0);
            // 1. UUID 기반 임시 파일명 생성
            String uuid = UUID.randomUUID().toString();
            tempXlsx = Files.createTempFile(uuid, ".xlsx");
            tempPdf = Path.of(tempXlsx.toString().replace(".xlsx", ".pdf"));
            // 2. 엑셀 템플릿 불러오기 및 수정(설치자재 출하 및 인수확인서)
            System.out.println(">>> PDF 응답 직전");
            try (FileInputStream fis = new FileInputStream("C:/Temp/mes21/문서/DeliveryReceipt.xlsx");
                 Workbook workbook = new XSSFWorkbook(fis);
                 FileOutputStream fos = new FileOutputStream(tempXlsx.toFile())) {

                Sheet sheet = workbook.getSheetAt(0);
                // 660 정보 setCell
                // HEAD 정보
                setCell(sheet, 4, 3, (String) head.get("PROCD"));           // D5
                setCell(sheet, 4, 8, (String) head.get("ACTNM"));           // I6
                String chulDateStr = head.get("CHULDATE") == null ? "" : head.get("CHULDATE").toString();
                setCell(sheet, 3, 5, formatToKorean((String) head.get("CHULDATE"))); // F4

                setCell(sheet, 6, 3, (String) head.get("cltnm"));              // D7
                setCell(sheet, 6, 8, (String) head.get("saupnum"));               // I7
                setCell(sheet, 7, 3, (String) head.get("prenm"));                 // D8
                setCell(sheet, 7, 8, (String) head.get("telnum"));   // I8
                setCell(sheet, 8, 3, (String) head.get("cltadres"));   // D9
                setCell(sheet, 9, 3, (String) head.get("biztypenm"));   // D10
                setCell(sheet, 9, 8, (String) head.get("bizitemnm"));   // I10
                setCell(sheet, 10, 3, (String) head.get("agnernm"));   // D11
                setCell(sheet, 10, 8, (String) head.get("agntel"));   // I11

                setCell(sheet, 34, 8, (String) head.get("sub_last_name"));              // I35
                setCell(sheet, 34, 4, (String) head.get("sub_cltnm"));               // E35
                setCell(sheet, 34, 7, (String) head.get("sub_phone"));                 // H35
                setCell(sheet, 34, 2, formatToDash((String) head.get("FACDATE")));   // C35
                setCell(sheet, 36, 2, formatToDash((String) head.get("FACDATE")));   // C37
                setCell(sheet, 38, 2, formatToDash((String) head.get("FACDATE")));   // C39

                // BODY 정보 (표 9행~29행, 최대 21개 row)
                for (int i = 0; i < 15; i++) {
                    int row = 14 + i; // 15행부터
                    if (i < vacData.size()) {
                        Map<String, Object> line = vacData.get(i);
                        setCell(sheet, row, 0, String.valueOf(i + 1));                  // A: 순번
                        setCell(sheet, row, 1, (String) line.get("PNAME"));             // B: 품명
                        setCell(sheet, row, 3, (String) line.get("PSIZE"));             // D: 규격
                        setCell(sheet, row, 5, (String) line.get("PUNIT"));             // F: 단위
                        setCell(sheet, row, 6, formatPrice(line.get("PQTY")));          // G: 수량 (콤마)
                        setCell(sheet, row, 7, formatPrice(line.get("PUAMT")));         // H: 단가 (콤마)
                        setCell(sheet, row, 8, formatPrice(line.get("PAMT")));         // I: 공급가액 (콤마)
//                        long pamt = line.get("PAMT") == null ? 0 : Long.parseLong(line.get("PAMT").toString());
//                        setCell(sheet, row, 12, formatPrice(pamt / 10));                // M: 세액 (10%)
                    } else {
                        // 빈 row
                        setCell(sheet, row, 0, "");
                        setCell(sheet, row, 1, "");
                        setCell(sheet, row, 3, "");
                        setCell(sheet, row, 5, "");
                        setCell(sheet, row, 6, "");
                        setCell(sheet, row, 7, "");
                        setCell(sheet, row, 8, "");
//                        setCell(sheet, row, 12, "");
                    }
                }

                workbook.write(fos);
            }
            System.out.println(">>> PDF 응답 후");

            // 3. LibreOffice로 PDF 변환
            ProcessBuilder pb = new ProcessBuilder(
                    "C:/Program Files/LibreOffice/program/soffice.exe",
                    "--headless",
                    "--convert-to", "pdf",
                    "--outdir", tempPdf.getParent().toString(),
                    tempXlsx.toAbsolutePath().toString()
            );
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();

            // 4. PDF 파일이 실제로 존재하는지 체크
            if (!Files.exists(tempPdf) || Files.size(tempPdf) == 0) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"PDF 생성 실패\"}");
                response.flushBuffer();
                return;
            }

            // 5. PDF 응답 전송 (정상 동작)
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=vacation.pdf");
            try (FileInputStream fis = new FileInputStream(tempPdf.toFile())) {
                IOUtils.copy(fis, response.getOutputStream());
                response.flushBuffer();
            }
        } catch (Exception e) {
            // 예외 발생 시 명확한 메시지 반환
            System.out.println(">>> 예외 발생: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"서버 에러: " + e.getMessage() + "\"}");
            response.flushBuffer();
        } finally {
            // 6. 임시 파일 삭제 (즉시)
            final Path deleteXlsx = tempXlsx;
            final Path deletePdf = tempPdf;

            Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                try { if (deleteXlsx != null) Files.deleteIfExists(deleteXlsx); } catch (Exception ex) {}
                try { if (deletePdf != null) Files.deleteIfExists(deletePdf); } catch (Exception ex) {}
            }, 5, TimeUnit.MINUTES);
        }
    }


    private void setCell(Sheet sheet, int rowIdx, int colIdx, String value) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) row = sheet.createRow(rowIdx);
        Cell cell = row.getCell(colIdx);
        if (cell == null) cell = row.createCell(colIdx);
        cell.setCellValue(value);
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


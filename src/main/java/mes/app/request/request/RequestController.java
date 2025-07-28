package mes.app.request.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mes.app.request.request.service.RequestService;
import mes.config.Settings;
import mes.domain.entity.User;
import mes.domain.entity.actasEntity.*;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/request/request")
public class RequestController {
    @Autowired
    private RequestService requestService;


    @Autowired
    private TB_DA007WRepository tbda007WRepository;

    @Autowired
    private TB_DA006WRepository tbda006WRepository;

    @Autowired
    private TB_DA006WFILERepository tbDa006WFILERepository;

    @Autowired
    Settings settings;
    // 주문출고 그리드 read
    @GetMapping("/order_read")
    public AjaxResult getOrderList(@RequestParam(value = "search_startDate", required = false) String searchStartDate,
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
        List<Map<String, Object>> items = this.requestService.getOrderList(spjangcd,
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
    // 주문출고 flag값 update
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
                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            } catch (JsonProcessingException e) {
                // 에러시 값, 에러 메시지 출력
                System.out.println("파싱에러: " + e.getMessage());
                sendData = new ArrayList<>(); // 혹은 적절히 처리
            }
        }
        try {
            for (Map<String, Object> item : sendData) {
                String chulFlag = normalizeChulFlag(item.get("CHULFLAG"));
                Integer baljuNum = toInt(item.get("BALJUNUM"));
                Integer baljuSeq = toInt(item.get("BALJUSEQ"));

                // 1. 기존값 조회 (null이면 0 취급, 필요시 변환)
                String oldFlag = (String) requestService.getChulFlag(baljuNum, baljuSeq).get("CHULFLAG");
                if (oldFlag == null) oldFlag = "0";

                // 2. 값이 다를 때만 처리
                if (!Objects.equals(oldFlag, chulFlag)) {
                    if ("1".equals(chulFlag)) { // 출고처리
                        // 0 → 1
                        requestService.updateChulInfo(username, today, chulFlag, baljuNum, baljuSeq);
                    } else { // 출고 해제
                        // 1 → 0
                        requestService.clearChulInfo("0", baljuNum, baljuSeq);
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

            final int MAX_ROW = 21; // 혹은 15
            int fileCount = (int) Math.ceil(vacData.size() / (double) MAX_ROW);

            // 임시파일, PDF 목록
            List<Path> pdfList = new ArrayList<>();

            Map<String, Object> head = vacData.get(0);

            for (int fileIdx = 0; fileIdx < fileCount; fileIdx++) {
                int fromIdx = fileIdx * MAX_ROW;
                int toIdx = Math.min(fromIdx + MAX_ROW, vacData.size());
                List<Map<String, Object>> subList = vacData.subList(fromIdx, toIdx);

                // 1. UUID 기반 임시 파일명 생성
                String uuid = UUID.randomUUID().toString();
                Path tempXlsx = Files.createTempFile(uuid, ".xlsx");
                Path tempPdf = Path.of(tempXlsx.toString().replace(".xlsx", ".pdf"));

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


                    // BODY 정보 (표 9행~29행, 최대 24개 row)
                    for (int i = 0; i < MAX_ROW; i++) {
                        int row = 8 + i; // B9부터
                        int globalIndex = fromIdx + i; // 전체 vacData 기준
                        if (i < subList.size()) {
                            Map<String, Object> line = subList.get(i);
                            String chulFlag = ((String) line.get("CHULFLAG")).equals("1") ? "확인" : "미출하";
                            setCell(sheet, row, 0, String.valueOf(globalIndex + 1));      // 번호, A9~A32
                            setCell(sheet, row, 1, (String) line.get("PNAME"));        // 품목, B9~B32
                            setCell(sheet, row, 5, (String) line.get("PSIZE"));        // 규격, F9~F32
                            setCell(sheet, row, 9, (String) line.get("PUNIT"));        // 단위, J9~J32
                            setCell(sheet, row, 11, line.get("PQTY").toString());        // 수량, L9~L32
                            setCell(sheet, row, 13, chulFlag);    // 출하여부, N9~N32
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

                if (!Files.exists(tempPdf) || Files.size(tempPdf) == 0) {
                    continue; // 생성실패 skip
                }
                pdfList.add(tempPdf);
                // 엑셀파일 삭제 예약 추가
                final Path deleteXlsx = tempXlsx;
                Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                    try { Files.deleteIfExists(deleteXlsx); } catch (Exception ex) {}
                }, 5, TimeUnit.MINUTES);
            }

                // 5. PDF 응답 전송 (정상 동작)
                if (pdfList.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"PDF 생성 실패\"}");
                    response.flushBuffer();
                    return;
                }
                response.setContentType("application/zip");
                response.setHeader("Content-Disposition", "attachment; filename=files.zip");
                try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                    int i = 1;
                    for (Path pdf : pdfList) {
                        zos.putNextEntry(new ZipEntry("파일" + i + ".pdf"));
                        Files.copy(pdf, zos);
                        zos.closeEntry();
                        i++;
                    }
                    zos.finish();
                    response.flushBuffer();
                }
                // --- 임시파일 삭제 예약 ---
                for (Path pdf : pdfList) {
                    Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                        try {
                            Files.deleteIfExists(pdf);
                        } catch (Exception ex) {
                        }
                    }, 5, TimeUnit.MINUTES);
                }
        } catch (Exception e) {
            // 예외 발생 시 명확한 메시지 반환
            System.out.println(">>> 예외 발생: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"서버 에러: " + e.getMessage() + "\"}");
            response.flushBuffer();
        }
    }
    // 엑셀파일 조회 및 파일 보기 메서드(거래명세서)
    @GetMapping("/readVacFile2")
    public void readVacFile2(@RequestParam(value = "search_Date", required = false) String searchDate,
                             @RequestParam(value = "search_actnm", required = false) String searchActnm,
//                            @RequestParam(value = "spjangcd", required = false) String spjangcd,
                             HttpServletResponse response,
                             Authentication auth) throws Exception {
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

            final int MAX_ROW = 15; //
            int fileCount = (int) Math.ceil(vacData.size() / (double) MAX_ROW);

            // 임시파일, PDF 목록
            List<Path> pdfList = new ArrayList<>();

            Map<String, Object> head = vacData.get(0);

            for (int fileIdx = 0; fileIdx < fileCount; fileIdx++) {
                int fromIdx = fileIdx * MAX_ROW;
                int toIdx = Math.min(fromIdx + MAX_ROW, vacData.size());
                List<Map<String, Object>> subList = vacData.subList(fromIdx, toIdx);

                // 1. UUID 기반 임시 파일명 생성
                String uuid = UUID.randomUUID().toString();
                Path tempXlsx = Files.createTempFile(uuid, ".xlsx");
                Path tempPdf = Path.of(tempXlsx.toString().replace(".xlsx", ".pdf"));

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

                    setCell(sheet, 34, 8, (String) head.get("last_name"));              // I35
                    setCell(sheet, 34, 4, (String) head.get("cltnm"));               // E35
                    setCell(sheet, 34, 7, (String) head.get("phone"));                 // H35
                    setCell(sheet, 34, 2, formatToDash((String) head.get("CHULDATE")));   // C35
                    setCell(sheet, 36, 2, formatToDash((String) head.get("CHULDATE")));   // C37
                    setCell(sheet, 38, 2, formatToDash((String) head.get("CHULDATE")));   // C39
                    // 1. 합계 계산 변수 선언
                    long totalQty = 0;     // G30: 수량합계
                    long totalPrice = 0;   // H30: 단가합계 (이건 행별 단가라면 합계가 아닌 보통 공란이나 평균 넣음)
                    long totalSupply = 0;  // I30: 공급가액합계
                    long totalTax = 0;     // J30: 세액합계
                    // BODY 정보 (표 9행~29행, 최대 21개 row)
                    for (int i = 0; i < MAX_ROW; i++) {
                        int globalIndex = fromIdx + i; // 전체 vacData 기준
                        int row = 14 + i; // 15행부터
                        if (i < subList.size()) {
                            Map<String, Object> line = subList.get(i);
                            long qty = line.get("PQTY") == null ? 0 : Long.parseLong(line.get("PQTY").toString());
                            long price = line.get("PUAMT") == null ? 0 : Long.parseLong(line.get("PUAMT").toString());
                            long supply = line.get("PAMT") == null ? 0 : Long.parseLong(line.get("PAMT").toString());
                            long tax = supply / 10;
                            // 합계 계산
                            totalQty += qty;
                            totalPrice += price;
                            totalSupply += supply;
                            totalTax += tax;

                            setCell(sheet, row, 0, String.valueOf(globalIndex + 1));                  // A: 순번
                            setCell(sheet, row, 1, (String) line.get("PNAME"));             // B: 품명
                            setCell(sheet, row, 3, (String) line.get("PSIZE"));             // D: 규격
                            setCell(sheet, row, 5, (String) line.get("PUNIT"));             // F: 단위
                            setCell(sheet, row, 6, formatPrice(line.get("PQTY")));          // G: 수량 (콤마)
                            setCell(sheet, row, 7, formatPrice(line.get("PUAMT")));         // H: 단가 (콤마)
                            setCell(sheet, row, 8, formatPrice(line.get("PAMT")));         // I: 공급가액 (콤마)
                            long pamt = line.get("PAMT") == null ? 0 : Long.parseLong(line.get("PAMT").toString());
                            setCell(sheet, row, 9, formatPrice(pamt / 10));                // J : 세액 (10%)
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
                    // 2. 합계 행에 값 넣기 (G30~J30은 row 29)
                    setCell(sheet, 29, 6, formatPrice(totalQty));      // G30: 수량합계
                    setCell(sheet, 29, 7, "");                        // H30: 단가합계(일반적으로 미사용)
                    setCell(sheet, 29, 8, formatPrice(totalSupply));   // I30: 공급가액합계
                    setCell(sheet, 29, 9, formatPrice(totalTax));      // J30: 세액합계

                    // 3. D12(11,3)에 총합계 (공급가액+세액)
                    setCell(sheet, 11, 3, formatPrice(totalSupply + totalTax));  // D12: 총합계

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

                if (!Files.exists(tempPdf) || Files.size(tempPdf) == 0) {
                    continue; // 생성실패 skip
                }
                pdfList.add(tempPdf);
                // 엑셀파일 삭제 예약 추가
                final Path deleteXlsx = tempXlsx;
                Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                    try {
                        Files.deleteIfExists(deleteXlsx);
                    } catch (Exception ex) {
                    }
                }, 5, TimeUnit.MINUTES);
            }

            // 5. PDF 응답 전송 (정상 동작)
            if (pdfList.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"PDF 생성 실패\"}");
                response.flushBuffer();
                return;
            }
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=files.zip");
            try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                int i = 1;
                for (Path pdf : pdfList) {
                    zos.putNextEntry(new ZipEntry("파일" + i + ".pdf"));
                    Files.copy(pdf, zos);
                    zos.closeEntry();
                    i++;
                }
                zos.finish();
                response.flushBuffer();
            }
            // --- 임시파일 삭제 예약 ---
            for (Path pdf : pdfList) {
                Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                    try {
                        Files.deleteIfExists(pdf);
                    } catch (Exception ex) {
                    }
                }, 5, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            // 예외 발생 시 명확한 메시지 반환
            System.out.println(">>> 예외 발생: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"서버 에러: " + e.getMessage() + "\"}");
            response.flushBuffer();
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
//    @PostMapping("/downloader")
//    public void downloadFile(
//            @RequestParam(value = "search_Date", required = false) String searchDate,
//            @RequestParam(value = "search_actnm", required = false) String searchActnm,
////                            @RequestParam(value = "spjangcd", required = false) String spjangcd,
//            HttpServletResponse response,
//            Authentication auth
//    ) throws IOException, InterruptedException {
//        User user = (User) auth.getPrincipal();
//        String username = user.getUsername();
//        String spjangcd = "ZZ";
//
//        Map<String, Object> userInfo = requestService.getUserInfo(username);
//        String cltcd = userInfo != null ? (String) userInfo.get("cltcd") : null;
//
//        // 주문출고 리스트 조회
//        String search_Date = (searchDate).replaceAll("-", "");
//
//        List<Map<String, Object>> vacData = requestService.getVacFileList(searchDate, searchActnm, cltcd, spjangcd);
//
//
//        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
//        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("yyyy 년 MM 월 dd 일");
//
//        LocalDate frDate = LocalDate.parse(frdateStr, ymdFormatter);
//        LocalDate toDate = LocalDate.parse(todateStr, ymdFormatter);
//        LocalDate reqDate = LocalDate.parse(reqdateStr, ymdFormatter);
//
//        String repodateFormat = String.format("%s  ~  %s  ( %s ) 일간",
//                frDate.format(displayFormatter),
//                toDate.format(displayFormatter),
//                daynum);
//
//        String uuid = UUID.randomUUID().toString();
//        Path tempXlsx = Files.createTempFile(uuid, ".xlsx");
//        Path tempPdf = Path.of(tempXlsx.toString().replace(".xlsx", ".pdf"));
//
//        try (FileInputStream fis = new FileInputStream("C:/Temp/mes21/문서/VacDemoFile.xlsx");
//             Workbook workbook = new XSSFWorkbook(fis);
//             FileOutputStream fos = new FileOutputStream(tempXlsx.toFile())) {
//
//            Sheet sheet = workbook.getSheetAt(0);
//            setCell(sheet, 2, 2, (String) vacData.get("worknm"));
//            setCell(sheet, 9, 2, (String) vacData.get("repopernm"));
//            setCell(sheet, 7, 2, (String) vacData.get("jiknm"));
//            setCell(sheet, 5, 2, (String) vacData.get("departnm"));
//            setCell(sheet, 16, 0, (String) vacData.get("remark"));
//            setCell(sheet, 11, 2, repodateFormat);
//            setCell(sheet, 24, 3, (String) vacData.get("worknm"));
//            setCell(sheet, 27, 0, reqDate.format(displayFormatter));
//            setCell(sheet, 30, 10, (String) vacData.get("repopernm"));
//
//            workbook.write(fos);
//        }
//
//        ProcessBuilder pb = new ProcessBuilder(
//                "C:/Program Files/LibreOffice/program/soffice.exe",
//                "--headless",
//                "--convert-to", "pdf",
//                "--outdir", tempPdf.getParent().toString(),
//                tempXlsx.toAbsolutePath().toString()
//        );
//        pb.inheritIO();
//        Process process = pb.start();
//        process.waitFor();
//
//        int retries = 0;
//        while ((!Files.exists(tempPdf) || Files.size(tempPdf) == 0) && retries++ < 100) {
//            Thread.sleep(100); // 최대 10초 대기
//        }
//        if (!Files.exists(tempPdf) || Files.size(tempPdf) == 0) {
//            throw new FileNotFoundException("PDF 변환 실패: " + tempPdf.toString());
//        }
//
//        try (FileInputStream fis = new FileInputStream(tempPdf.toFile())) {
//            response.setContentType("application/pdf");
//            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''휴가신청서.pdf");
//            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
//            response.setHeader("Pragma", "no-cache");
//            response.setHeader("Expires", "0");
//
//            IOUtils.copy(fis, response.getOutputStream());
//            response.flushBuffer();
//        }
//        // ⬇ 여기서 executor 실행
//        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
//        cleaner.schedule(() -> {
//            try {
//                Files.deleteIfExists(tempXlsx);
//                Files.deleteIfExists(tempPdf);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }, 5, TimeUnit.MINUTES);
//        cleaner.shutdown();
//    }

}


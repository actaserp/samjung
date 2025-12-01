package mes.app.douzone_service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mes.app.douzone_service.service.APIDouZoneService;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/api_DouZoneService")
public class APIDouZoneServiceController { //더존 api 연동서비스

	@Autowired
	APIDouZoneService apidouZoneService;
	@Autowired
	DouzoneClient douzoneClient;
	@Autowired
	ObjectMapper objectMapper;

	@GetMapping("/sales_read")
	public AjaxResult getSalesRead(@RequestParam("start")    String start,
																 @RequestParam("end")      String end,
																 @RequestParam("company")  String company,
																 @RequestParam("sale_type")String sale_type,
																 Authentication auth) {
		User user = (User)auth.getPrincipal();
		String spjangcd = user.getSpjangcd();
		List<Map<String, Object>> items =
			this.apidouZoneService.getSalesRead(start, end, company, sale_type, spjangcd);

		AjaxResult result = new AjaxResult();
		result.data = items;

		return result;
	}

	@PostMapping("/DouZoneSave")
	public AjaxResult salesDouZoneSave(@RequestBody Map<String, Object> body) {	//매출 전송[저장]처리
		AjaxResult result = new AjaxResult();

		try {
//			log.info("sales_DouZoneSave req: {}", body);

			String coCd   = (String) body.getOrDefault("coCd", "1000");
			Object docuTyObj = body.get("docuTy");
			String docuTy = (docuTyObj != null) ? docuTyObj.toString().trim() : null;

			if (docuTy == null || docuTy.isEmpty()) {
				result.success = false;
				result.message = "전표유형(docuTy)이 없습니다. (2: 매입, 3: 매출, 4: 수금, 5: 지급)";
				return result;
			}

			// 🔹 허용값(2,3,4,5)만 사용하도록 검증(선택)
			if (!docuTy.equals("2") && !docuTy.equals("3")
						&& !docuTy.equals("4") && !docuTy.equals("5")) {
				result.success = false;
				result.message = "전표유형(docuTy)이 올바르지 않습니다. (받은 값: " + docuTy + ")";
				return result;
			}

			@SuppressWarnings("unchecked")
			List<Map<String, Object>> lines =
				(List<Map<String, Object>>) body.get("data");

			if (lines == null || lines.isEmpty()) {
				result.success = false;
				result.message = "전송할 데이터가 없습니다.";
				return result;
			}

			// ⓪ 사내 계정코드/계정명 → 더존 계정코드(acctCd) 매핑 (11A02 사용)
			//    - acctCd가 7자리면 이미 더존 코드라고 보고 스킵
			//    - acctCd가 4~5자리(내부 코드)거나 비어 있으면 acctNm 기준으로 11A02 LIKE 검색
			List<String> mapErrors = apidouZoneService.applyAccountCodeByAcctNm(coCd, lines);
			if (!mapErrors.isEmpty()) {
				result.success = false;
				result.message = "계정 매핑 오류:\n" + String.join("\n", mapErrors);
				return result;
			}

			// ① 계정 + 관리항목 사전 검증 (여기서는 이미 더존 acctCd 기준으로 검증 가능)
			List<String> errors = apidouZoneService.validateAccountsAndControls(coCd, lines);
			if (!errors.isEmpty()) {
				result.success = false;
				result.message = "전송 전 검증 오류:\n" + String.join("\n", errors);
				return result;
			}

			// ② menuSq 채번 + docuTy 세팅
			String menuDt = (String) lines.get(0).get("menuDt");
			int menuSq    = apidouZoneService.generateMenuSqFromDb(coCd, menuDt);

			for (Map<String, Object> line : lines) {
				line.put("menuSq", menuSq);
				line.put("docuTy", docuTy);
			}

			// ③ 더존 자동전표등록 호출 (11A10)
			Map<String, Object> payload = new HashMap<>();
			payload.put("coCd",   coCd);
//			payload.put("menuCd", menuCd);
			payload.put("data",   lines);

			Map<String, Object> dzRes = douzoneClient.callAutoDocuSave(payload);

			Object rcObj = dzRes.get("resultCode");
			Integer resultCode = null;
			if (rcObj instanceof Number) {
				resultCode = ((Number) rcObj).intValue();
			} else if (rcObj != null) {
				resultCode = Integer.parseInt(rcObj.toString());
			}

			String resultMsg = (String) dzRes.get("resultMsg");

			if (resultCode != null && resultCode == 0) {

				try {
					switch (docuTy) {
						case "3": // 매출
							apidouZoneService.updateSalesSendFlag(lines, menuDt, menuSq);
							break;

						case "2": // 비용(매입)
							apidouZoneService.updatePriceSendFlag(lines, menuDt, menuSq);
							break;

						case "4": // 수금
							apidouZoneService.updateReceiptSendFlag(lines, menuDt, menuSq);
							break;

						case "5": // 지급
							apidouZoneService.updatePaymentSendFlag(lines, menuDt, menuSq);
							break;

						default:
							log.warn("전표유형(docuTy={})에 대한 플래그 업데이트 로직이 없습니다.", docuTy);
					}
				} catch (Exception e) {
					log.warn("TB_DA0xx DATASEND_DIVISION 업데이트 실패", e);
				}

				result.success = true;
				result.message = "더존 자동전표 전송 완료";
				result.data    = dzRes;
			} else {
				result.success = false;
				result.message = "더존 오류: " + (resultMsg != null ? resultMsg : "알 수 없는 오류");
				result.data    = dzRes;
			}

		} catch (Exception e) {
			log.error("sales_DouZoneSave error", e);
			result.success = false;
			result.message = "전송 중 오류: " + e.getMessage();
		}

		return result;
	}


	@GetMapping("/price_read")
	public AjaxResult getPriceRead(@RequestParam(value = "start")String start,
																 @RequestParam(value = "end")String end,
																 @RequestParam(value = "company")String company,
																 @RequestParam(value = "price_type")String price_type,
																 Authentication auth
																 ) {

		User user = (User)auth.getPrincipal();
		String spjangcd = user.getSpjangcd();

		List<Map<String, Object>> items = this.apidouZoneService.getPriceRead(start, end, company,price_type, spjangcd);

		AjaxResult result = new AjaxResult();
		result.data = items;

		return result;
	}

	@GetMapping("/receipt_read")
	public AjaxResult getReceiptRead(@RequestParam(value = "start")String start,
																 @RequestParam(value = "end")String end,
																 @RequestParam(value = "company")String company,
																 @RequestParam(value = "receipt_type")String receipt_type,
																	 Authentication auth
																 ) {
		User user = (User)auth.getPrincipal();
		String spjangcd = user.getSpjangcd();
		List<Map<String, Object>> items = this.apidouZoneService.getReceiptRead(start, end, company,receipt_type, spjangcd);

		AjaxResult result = new AjaxResult();
		result.data = items;

		return result;
	}

	@GetMapping("/payment_read")
	public AjaxResult getPaymentRead(@RequestParam(value = "start")String start,
																 @RequestParam(value = "end")String end,
																 @RequestParam(value = "company")String company,
																 @RequestParam(value = "payment_type")String payment_type,
																	 Authentication auth
																 ) {
		User user = (User)auth.getPrincipal();
		String spjangcd = user.getSpjangcd();

		List<Map<String, Object>> items = this.apidouZoneService.getPaymentRead(start, end, company,payment_type, spjangcd);

		AjaxResult result = new AjaxResult();
		result.data = items;

		return result;
	}

	@GetMapping("/sales_DouZone_list")
	public AjaxResult getDouZoneSalesRead(@RequestParam("start")   String start,
																				@RequestParam("end")     String end,
																				@RequestParam(value = "company",required = false) String company,
																				@RequestParam("docuTy")  String docuTy) {

		AjaxResult result = new AjaxResult();

		try {
			// 화면 날짜(yyyy-MM-dd) → 더존 포맷(yyyyMMdd)
			String frDt = start != null ? start.replaceAll("-", "") : "";
			String toDt = end   != null ? end.replaceAll("-", "")   : "";

			String coCd   = "1000";
			String divCd  = "1000";
			int viewPage  = 1;
			int viewCount = 50;

			// 🔹 여기서는 docuTy를 더존에 안 보내고, 우리 쪽에서 필터
			String douzoneResponse =
				douzoneClient.callAutoDocuList(coCd, divCd, frDt, toDt, docuTy, viewPage, viewCount);

//			log.info("[API] 더존 응답 RAW: {}", douzoneResponse);

			JsonNode root     = objectMapper.readTree(douzoneResponse);
			JsonNode dataNode = root.path("resultData").path("data");

			List<Map<String, Object>> rows = objectMapper.convertValue(
				dataNode,
				new TypeReference<List<Map<String, Object>>>() {}
			);

			// ✅ 1) docuTy 기준으로 먼저 필터링
			String docuTyFilter = docuTy != null ? docuTy.trim() : "";
			List<Map<String, Object>> filteredRows = rows;
			if (!docuTyFilter.isEmpty()) {
				filteredRows = rows.stream()
												 .filter(r -> docuTyFilter.equals(
													 String.valueOf(r.getOrDefault("docuTy", ""))
												 ))
												 .toList();
			}

			// 11A16 원본 rows -> 화면용 rows 변환
			List<Map<String, Object>> viewRows = new ArrayList<>();
			int rownum = 1;

			// ✅ 2) rows 대신 filteredRows 사용
			for (Map<String, Object> r : filteredRows) {
				Map<String, Object> v = new HashMap<>();

				String menuDt = String.valueOf(r.getOrDefault("menuDt", ""));
				String menuSq = String.valueOf(r.getOrDefault("menuSq", ""));
				String menuLnSq = String.valueOf(r.getOrDefault("menuLnSq", ""));
				String isuDt  = String.valueOf(r.getOrDefault("isuDt", ""));
				String trCd   = String.valueOf(r.getOrDefault("trCd", ""));
				String trNm   = String.valueOf(r.getOrDefault("trNm", ""));
				String acctCd = String.valueOf(r.getOrDefault("acctCd", ""));
				String isuDoc = String.valueOf(r.getOrDefault("isuDoc", ""));
				String rmkDc = String.valueOf(r.getOrDefault("rmkDc", ""));
				Object acctAm = r.get("acctAm");

				// 공통: 작성일자 포맷
				String yymmdd = "";
				if (!menuDt.isEmpty() && menuDt.length() == 8) {
					String yy = menuDt.substring(2, 4);
					String MM = menuDt.substring(4, 6);
					String dd = menuDt.substring(6, 8);
					yymmdd = yy + "." + MM + "." + dd;
				}
				String seqStr  = (!menuSq.isEmpty() ? String.format("%04d", Integer.parseInt(menuSq)) : "");
				String dateSeq = yymmdd.isEmpty() ? "" : (yymmdd + " - " + seqStr);

				v.put("rownum", rownum++);
				v.put("menuDt", menuDt);
				v.put("menuSq", menuSq);
				v.put("isuDoc", isuDoc);

				// 🔹 docuTy별 그리드 매핑 (프론트에서 온 docuTy 사용)
				if ("3".equals(docuTyFilter)) {         // 매출 탭
					v.put("saleDate",  dateSeq);
					v.put("custCode",  trCd);
					v.put("custName",  trNm);
					v.put("issueDate", menuDt);
					v.put("acccd",     acctCd);
					v.put("supplyAmt", null);
					v.put("vatAmt",    null);
					v.put("totalAmt",  acctAm);
					v.put("doozenNo",  menuSq);
					//v.put("saleType",  rmkDc);

				} else if ("2".equals(docuTyFilter)) {  // 비용 탭 (매입)
					v.put("mijDate",   dateSeq);
					v.put("cltcd",     trCd);
					v.put("actcd",     "");
					v.put("cltnm",     trNm);
					v.put("PriceDate", menuDt);
					v.put("djacccd",   acctCd);
					v.put("accnm",     acctCd);
					v.put("amt",       0);
					v.put("addamt",    0);
					v.put("totalAmt",  acctAm);
					v.put("dzIssueNo",  menuSq);

				} else if ("4".equals(docuTyFilter)) {  // 수금 탭
					v.put("relation_no",   dateSeq);
					v.put("CUSTOMER_CD",   trCd);
					v.put("cltcd",         "");
					v.put("CUSTOMER_NAME", trNm);
					v.put("RELATION_DATE", yymmdd);
					v.put("djacccd",       acctCd);
					v.put("accnm",         acctCd);
					v.put("SUPPLY_PRICE",  0);
					v.put("SURTAX",        0);
					v.put("TOTAL_AMOUNT",  acctAm);
					v.put("IN_DT_SEQ",  menuSq);

				} else if ("5".equals(docuTyFilter)) {  // 지급 탭
					v.put("relation_no",   dateSeq);
					v.put("CUSTOMER_CD",   trCd);
					v.put("cltcd",         "");
					v.put("CUSTOMER_NAME", trNm);
					v.put("RELATION_DATE", isuDt);
					v.put("SUPPLY_PRICE",  0);
					v.put("SURTAX",        0);
					v.put("TOTAL_AMOUNT",  acctAm);
					v.put("IN_DT_SEQ",  menuSq);
				}

				viewRows.add(v);
			}

			if (!rows.isEmpty()) {
//				log.info("[API] 더존 11A16 첫 로우 컬럼들: {}", rows.get(0).keySet());
			}

			result.success = true;
			result.data    = viewRows;

		} catch (Exception e) {
			log.error("[API] /sales_DouZone_list 호출 중 오류", e);
			result.success = false;
			result.message = e.getMessage();
		}

		return result;
	}

	@PostMapping("/sales_DouZoneDelete")
	public AjaxResult deleteDouZoneSales(@RequestBody Map<String, Object> body) {
		AjaxResult result = new AjaxResult();

		try {
			String coCd = (String) body.getOrDefault("coCd", "1000");

			// 🔹 docuTy (2: 비용, 3: 매출, 4: 수금, 5: 지급)
			Object docuTyObj = body.get("docuTy");
			String docuTy = (docuTyObj != null) ? docuTyObj.toString().trim() : null;

			if (docuTy == null || docuTy.isEmpty()) {
				result.success = false;
				result.message = "전표유형(docuTy)이 없습니다. (2: 매입, 3: 매출, 4: 수금, 5: 지급)";
				return result;
			}

			// 🔹 1) 다건 삭제(items 배열로 들어온 경우)
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");

			if (items != null && !items.isEmpty()) {
				int successCnt = 0;
				int failCnt    = 0;
				List<Map<String, Object>> dzResponses = new ArrayList<>();

				for (Map<String, Object> item : items) {
					String menuDt = String.valueOf(item.get("menuDt"));
					Object menuSqObj = item.get("menuSq");
					Integer menuSq = (menuSqObj instanceof Number)
														 ? ((Number) menuSqObj).intValue()
														 : Integer.parseInt(String.valueOf(menuSqObj));

					Map<String, Object> payload = new HashMap<>();
					payload.put("coCd",   coCd);
					payload.put("menuDt", menuDt);
					payload.put("menuSq", menuSq);

					Map<String, Object> dzRes = douzoneClient.callAutoDocuDelete(payload);
					dzResponses.add(dzRes);

					Object rcObj = dzRes.get("resultCode");
					int resultCode = (rcObj instanceof Number)
														 ? ((Number) rcObj).intValue()
														 : Integer.parseInt(String.valueOf(rcObj));
					String resultMsg = (String) dzRes.get("resultMsg");

					if (resultCode == 0) {
						successCnt++;

						// 🔹 더존 전표 삭제 성공 → 우리 쪽 플래그 원복
						try {
							switch (docuTy) {
								case "3": // 매출
									apidouZoneService.resetSalesSendFlag(menuDt, menuSq);
									break;
								case "2": // 비용(매입)
									apidouZoneService.resetPriceSendFlag(menuDt, menuSq);
									break;
								case "4": // 수금
									apidouZoneService.resetReceiptSendFlag(menuDt, menuSq);
									break;
								case "5": // 지급
									apidouZoneService.resetPaymentSendFlag(menuDt, menuSq);
									break;
								default:
									log.warn("지원하지 않는 docuTy 삭제 플래그 원복 요청: {}", docuTy);
							}
						} catch (Exception e) {
							log.warn("전송플래그 원복 실패 (docuTy={}, menuDt={}, menuSq={})",
								docuTy, menuDt, menuSq, e);
						}

					} else {
						failCnt++;
						log.warn("더존 삭제 오류 (menuDt={}, menuSq={}): {}", menuDt, menuSq,
							resultMsg != null ? resultMsg : "알 수 없는 오류");
					}
				}

				if (failCnt == 0) {
					result.success = true;
					result.message = String.format("더존 자동전표 삭제 완료 (%d건)", successCnt);
				} else if (successCnt == 0) {
					result.success = false;
					result.message = "모든 더존 전표 삭제에 실패했습니다.";
				} else {
					result.success = false;
					result.message =
						String.format("일부 삭제 실패: 성공 %d건 / 실패 %d건", successCnt, failCnt);
				}

				result.data = dzResponses;
				return result;
			}

			// 🔹 2) 기존 단건 삭제 (menuDt, menuSq 단일 값으로 들어온 경우 - 백워드 호환용)
			String menuDt = (String) body.get("menuDt");
			Object menuSqObj = body.get("menuSq");

			if (menuDt == null || menuSqObj == null) {
				result.success = false;
				result.message = "삭제할 전표 정보가 없습니다.(menuDt/menuSq)";
				return result;
			}

			Integer menuSq = (menuSqObj instanceof Number)
												 ? ((Number) menuSqObj).intValue()
												 : Integer.parseInt(String.valueOf(menuSqObj));

			Map<String, Object> payload = new HashMap<>();
			payload.put("coCd",   coCd);
			payload.put("menuDt", menuDt);
			payload.put("menuSq", menuSq);

			Map<String, Object> dzRes = douzoneClient.callAutoDocuDelete(payload);

			Object rcObj = dzRes.get("resultCode");
			int resultCode = (rcObj instanceof Number)
												 ? ((Number) rcObj).intValue()
												 : Integer.parseInt(String.valueOf(rcObj));
			String resultMsg = (String) dzRes.get("resultMsg");

			if (resultCode == 0) {
				try {
					switch (docuTy) {
						case "3": // 매출
							apidouZoneService.resetSalesSendFlag(menuDt, menuSq);
							break;
						case "2": // 비용(매입)
							apidouZoneService.resetPriceSendFlag(menuDt, menuSq);
							break;
						case "4": // 수금
							apidouZoneService.resetReceiptSendFlag(menuDt, menuSq);
							break;
						case "5": // 지급
							apidouZoneService.resetPaymentSendFlag(menuDt, menuSq);
							break;
						default:
							log.warn("지원하지 않는 docuTy 삭제 플래그 원복 요청: {}", docuTy);
					}
				} catch (Exception e) {
					log.warn("전송플래그 원복 실패 (docuTy={}, menuDt={}, menuSq={})",
						docuTy, menuDt, menuSq, e);
				}

				result.success = true;
				result.message = "더존 자동전표 삭제 완료";
				result.data    = dzRes;
			} else {
				result.success = false;
				result.message = "더존 삭제 오류: " + (resultMsg != null ? resultMsg : "알 수 없는 오류");
				result.data    = dzRes;
			}

		} catch (Exception e) {
			log.error("sales_DouZoneDelete error", e);
			result.success = false;
			result.message = "삭제 중 오류: " + e.getMessage();
		}

		return result;
	}


}

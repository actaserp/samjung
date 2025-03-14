package mes.app.order_status.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class VendorOrderStatsService {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getOrderStatusByOperid(String startDate, String endDate, String searchSpjangcd, String searchCltnm) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    StringBuilder sql = new StringBuilder("""
        SELECT
            tb006.*,
            uc.Value AS ordflag_display
        FROM
            TB_DA006W tb006
        left join user_code uc on uc.Code = tb006.ordflag
        WHERE 1=1
        """);
    if (startDate != null && !startDate.isEmpty()) {
      startDate = startDate.replace("-", "");
      sql.append(" AND reqdate >= :startDate");
      params.addValue("startDate", startDate);
    }

    if (endDate != null && !endDate.isEmpty()) {
      sql.append(" AND reqdate <= :endDate");
      params.addValue("endDate", endDate);
    }
    if (searchSpjangcd != null && !searchSpjangcd.isEmpty()) {
      sql.append(" AND spjangcd = :spjangcd");
      params.addValue("spjangcd", searchSpjangcd);
    }

    if (searchCltnm != null && !searchCltnm.isEmpty()) {
      sql.append(" AND cltnm LIKE :cltnm");
      params.addValue("cltnm", "%" + searchCltnm + "%");
    }
    sql.append(" ORDER BY reqdate DESC");

    //log.info("업체별 주문통계 그리드 read SQL: {}", sql.toString());
    //log.info("바인딩된 파라미터: {}", params.getValues());

    return sqlRunner.getRows(sql.toString(), params);
  }

  public List<Map<String, Object>> getChartData(String spjangcd, String startDate, String endDate, String searchCltnm) {

    MapSqlParameterSource params = new MapSqlParameterSource();
    StringBuilder sql = new StringBuilder("""
        SELECT cltnm, ordflag
        FROM tb_da006w
        WHERE 1=1
        """);

    if (spjangcd != null && !spjangcd.isEmpty()) {
      sql.append(" AND spjangcd = :spjangcd");
      params.addValue("spjangcd", spjangcd);
    }

    if (startDate != null && !startDate.isEmpty()) {
      startDate = startDate.replace("-", "");
      sql.append(" AND reqdate >= :startDate");
      params.addValue("startDate", startDate);
    }

    if (endDate != null && !endDate.isEmpty()) {
      sql.append(" AND reqdate <= :endDate");
      params.addValue("endDate", endDate);
    }

    if (searchCltnm != null && !searchCltnm.isEmpty()) {
      sql.append(" AND cltnm LIKE :cltnm");
      params.addValue("cltnm", "%" + searchCltnm + "%");
    }
    sql.append(" ORDER BY reqdate DESC");

    //log.info("업체별 주문통계 차트 read SQL: {}", sql.toString());
    //log.info("바인딩된 파라미터: {}", params.getValues());
    return sqlRunner.getRows(sql.toString(), params);
  }
}

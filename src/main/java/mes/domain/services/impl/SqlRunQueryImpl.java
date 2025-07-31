package mes.domain.services.impl;


import java.util.List;
import java.util.Map;

import org.hibernate.exception.DataException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import mes.domain.services.LogWriter;
import mes.domain.services.SqlRunner;


@Repository("sqlRunner")
@Primary
public class SqlRunQueryImpl implements SqlRunner {

	@Autowired(required = true)
	@Qualifier("namedParameterJdbcTemplate")
    private NamedParameterJdbcTemplate  jdbcTemplate;
	
	@Autowired
	LogWriter logWriter;
	
	
    public List<Map<String, Object>> getRows(String sql, MapSqlParameterSource dicParam){    	
    	
    	List<Map<String, Object>> rows = null;
    	
    	
    	try {
    		rows = this.jdbcTemplate.queryForList(sql, dicParam);
		} 
    	catch(DataAccessException de) {
    		System.out.println(de);
    	}
    	catch (Exception e) {
			// TODO: handle exception
			logWriter.addDbLog("error", "SqlRunQueryImpl.getRows", e);
		}
    	return rows;
    }
    
    public Map<String, Object> getRow(String sql, MapSqlParameterSource dicParam){    	

    	Map<String, Object> row = null;
    	
    	try {
    		row = this.jdbcTemplate.queryForMap(sql, dicParam);
		} 
    	catch(DataAccessException de) {
    		
    	
    	}
    	catch (Exception e) {
			// TODO: handle exception
			logWriter.addDbLog("error", "SqlRunQueryImpl.getRow", e);
		}
    	return row;
    }

	public int execute(String sql, MapSqlParameterSource dicParam) {
		try {
			return this.jdbcTemplate.update(sql, dicParam);
		} catch (Exception e) {
			logWriter.addDbLog("error", "SqlRunQueryImpl.execute", e);
			throw e; // 반드시 던져야 롤백됨
		}
	}
    
    public int queryForCount(String sql,  MapSqlParameterSource dicParam) {
    	//select count(*) from xxx where ~
    	return this.jdbcTemplate.queryForObject(sql, dicParam, int.class);
    }
    
    public <T> T queryForObject(String sql,  MapSqlParameterSource dicParam, RowMapper<T> mapper) throws DataException {
    	T rr= this.jdbcTemplate.queryForObject(sql, dicParam, mapper); 
    	return rr;    	
    }
    
}

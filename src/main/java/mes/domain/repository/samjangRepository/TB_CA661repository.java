package mes.domain.repository.samjangRepository;

import mes.domain.entity.samjungEntity.TB_CA661;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TB_CA661repository extends JpaRepository<TB_CA661, Integer> {

  @Modifying
  @Query("DELETE FROM TB_CA661 d WHERE d.balJunum.balJunum = :baljunum")
  void deleteByBaljunum(@Param("baljunum") Integer baljunum);

}

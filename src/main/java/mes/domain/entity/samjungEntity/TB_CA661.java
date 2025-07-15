package mes.domain.entity.samjungEntity;

import lombok.Data;
import lombok.NoArgsConstructor;
import mes.domain.entity.AbstractAuditModel;

import javax.persistence.*;

@Entity
@Table(name="TB_CA661")
@NoArgsConstructor
@Data
public class TB_CA661 { //외주발주상세관리
  @Id
  @Column(name = "BALJUSEQ")
  private Integer baljuseq; //발주상세번호

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "BALJUNUM", referencedColumnName = "BALJUNUM")
  private TB_CA660 tbCa660;   //발주번호

  @Column(name = "CUSTCD")
  private String custcd;  //회사코드

  @Column(name = "SPJANGCD")
  private String spjangcd;  //사업장코드

  @Column(name = "PROCD")
  private String procd;   //프로젝트번호

  @Column(name = "BALJUDATE")
  private String baljudate;   //발주일자

  @Column(name = "PCODE")
  private String pcode;   //품목코드

  @Column(name = "PNAME")
  private String pname;   //품명

  @Column(name = "PSIZE")
  private String psize;   //규격

  @Column(name = "PUNIT")
  private String punit;   //비고

  @Column(name = "PQTY")
  private Integer pqty;   //수량

  @Column(name = "PUAMT")
  private Integer puamt;  //단가

  @Column(name = "PAMT")
  private Integer pamt;   //금액

  @Column(name = "PMAPSEQ")
  private String pmapseq;   //도번

  @Column(name = "REMARK")
  private String remark;    //비고

  @Column(name = "CHULFLAG")
  private String chulflag;    //발주처출고여부

  @Column(name = "FACFLAG")
  private String facflag;     //공장확인여부

  @Column(name = "HYUNFLAG")
  private String hyunflag;    //현장확인여부

  @Column(name = "CHULDATE")
  private String chuldate;    //발주처출고일자

  @Column(name = "CHULPERNM")
  private String chulpernm;   //발주처출고자

  @Column(name = "FACDATE")
  private String facdate;     //공장확인일자

  @Column(name = "FACPERNM")
  private String facpernm;    //공장확인자

  @Column(name = "HYUNDATE")
  private String hyundate;    //현장확인일자

  @Column(name = "HYUNPERNM")
  private String hyunpernm;   //공장확인자
}

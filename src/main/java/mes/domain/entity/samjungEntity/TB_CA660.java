package mes.domain.entity.samjungEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name="TB_CA660")
@NoArgsConstructor
@Data
public class TB_CA660 { //외주발주관리

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name="BALJUNUM")
  Integer balJunum;  //발주번호

  @Column(name = "CUSTCD")
  private String custcd;    //회사코드

  @Column(name = "SPJANGCD")
  private String spjangcd;    //사업장코드

  @Column(name = "BALJUDATE")
  private String baljudate;  //발주일자

  @Column(name = "PROCD")
  private String procd;     //프로젝트번호

  @Column(name = "CLTCD")
  private String cltcd;     //발주처코드

  @Column(name = "ICHDATE")
  private String ichdate;   //납기일자(예정)

  @Column(name = "PERNM")
  private String pernm;     //현장PM

  @Column(name = "PERTELNO")
  private String pertelno;  //현장PM연락처

  @Column(name = "ACTCD")
  private String actcd;   //현장코드

  @Column(name = "ACTNM")
  private String actnm;   //현장명

  @Column(name = "ACTADDRESS")
  private String actaddress;  //현장주소

  @Column(name = "CLTPERNM")
  private String cltpernm;  //발주처담당자

  @Column(name = "CLTJIK")
  private String cltjik;  //발주처담당직위

  @Column(name = "CLTTELNO")
  private String clttelno;  //발주처담당연락처

  @Column(name = "CLTEMAIL")
  private String cltemail;  //발주담당이메일

  @Column(name = "REMARK01")
  private String remark01;  //특이사항1

  @Column(name = "REMARK02")
  private String remark02;  //특이사항2

  @Column(name = "REMARK03")
  private String remark03;  //특이사항3
}

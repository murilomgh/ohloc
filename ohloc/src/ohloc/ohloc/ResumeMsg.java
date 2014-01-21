/**
 * 
 * Copyright 2014 Jose Luiz da Silva ( luizjls@gmail.com )
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 */

package ohloc.ohloc;

/**
 * Este metodo representa de um resumo de uma mesagem ATS ( FPL, FPLs, DLA, CHG ou CNL )
 * @author Jose Luiz
 *
 */
public class ResumeMsg implements  Comparable<ResumeMsg> {
	

	public enum MsgTypeE {  // Tipos de mensagems FPL, Notivicacao, DLA, CHG e CNL    NO ( nao informado )
		FPL("FPL"), 
		FPLs("NTV"), 
		DLA("DLA"), 
		CHG("CHG"), 
		CNL("CNL"), 
		NO("NIL");    
		
		MsgTypeE(String value) {
		   this.value = value;
		}
		
		private String value;

		public String getValue() {
			return value;
		}
		
    }
	
	
	public enum ReportTypeE {  // Informa o tipo de relatorio estatistico que sera disponibilizado para o usuario
		SIMPLE("S"),  	// Relatorio simples
		MODERATE("M"), // Relatorio Moderado
		FULL("F");     // Relatorio Completo
		
		ReportTypeE(String value) {
			   this.value = value;
		}
		
		private String value;

		public String getValue() {
			return value;
		}
    }
	
	

	
	public enum ReportTitleE {  // Informa o titulo de forma a orientar o usuario para o significado para cada um das colunas para um relatorio estatistico gerado
		SIMPLE	("[NUMERO SEQUENCIAL DA MSG] [TIPO DE MENSAGEM -> FPL NTV CHG DLA CNL] [REGRA DE VOO -> IVYZ] [TYPO DA AERONAVE] [EOBT] [FROM] [ORIGEM] [DESTINO] [DATA DA APRESENTACAO -> DDMMAA] [HORA DA APRESENTACAO -> HH:MM:SS] [DATA HORA DA APRESENTACAO -> DD/MM/AAAA HH:MM:SS] [DATA HORA DA APRESENTACAO -> AAMMDDHHMMSS]"),  	
		MODERATE("[NUMERO SEQUENCIAL DA MSG] [MATRICULA DA AERONAVE] [TIPO DE MENSAGEM -> FPL NTV CHG DLA CNL] [REGRA DE VOO -> IVYZ] [TYPO DA AERONAVE] [EOBT] [FROM] [ORIGEM] [DESTINO] [DATA DA APRESENTACAO -> DDMMAA] [HORA DA APRESENTACAO -> HH:MM:SS] [DATA HORA DA APRESENTACAO -> DD/MM/AAAA HH:MM:SS] [DATA HORA DA APRESENTACAO -> AAMMDDHHMMSS]"), 
		FULL	("[NUMERO SEQUENCIAL DA MSG] [MATRICULA DA AERONAVE] [TIPO DE MENSAGEM -> FPL NTV CHG DLA CNL] [REGRA DE VOO -> IVYZ] [TYPO DA AERONAVE] [EOBT] [FROM] [ORIGEM] [DESTINO] [OPERADOR DA AERONAVE] [NOME CMTE] [ANAC CMTE] [NOME COPILOTO] [ANAC COPILOTO] [TELEFONE] [DATA DA APRESENTACAO -> DDMMAA] [HORA DA APRESENTACAO -> HH:MM:SS] [DATA HORA DA APRESENTACAO -> DD/MM/AAAA HH:MM:SS] [DATA HORA DA APRESENTACAO -> AAMMDDHHMMSS]");     
		
		 ReportTitleE(String value) {
			   this.value = value;
		}
		
		private String value;

		public String getValue() {
			return value;
		}
    }
	
	
	public enum OpE {  // Tipos da operacao  IFR, Visual, Y ou Z
		I("I"), 
		V("V"), 
		Y("Y"), 
		Z("Z"),  
		NO("");    
		
		OpE(String value) {
		   this.value = value;
		}
		
		private String value;

		public String getValue() {
			return value;
		}
		
    }
	
	
	/**
	 * Retorna a linha de titulo para o documento a ser exportado
	 * @return
	 */
	public static String getHeaderLineSimple( String separator ){
		return "REGISTRO" + separator + "TIPO_ATS" + separator + "REGRA_DE_VOO" + separator + "TYPO" + separator + "EOBT" + separator + "FROM" + separator + "ORIGEM" + separator + "DESTINO" + separator + "DATA_AP_01" + separator + "HORA_AP" + separator + "DATA_HORA_AP" + separator + "DATA_HORA_TXT";
	}
	
	/**
	 * Retorna a linha de titulo para o documento a ser exportado
	 * @return
	 */
	public static String getHeaderLineModerate( String separator ){
		return "REGISTRO" + separator + "MATRICULA" + separator + "TIPO_ATS" + separator + "REGRA_DE_VOO" + separator + "TYPO" + separator + "EOBT" + separator + "FROM" + separator + "ORIGEM" + separator + "DESTINO" + separator + "DATA_AP_01" + separator + "HORA_AP" + separator + "DATA_HORA_AP" + separator + "DATA_HORA_TXT";
	}
	
	/**
	 * Retorna a linha de titulo para o documento a ser exportado
	 * @return
	 */
	public static String getHeaderLineFull( String separator ){
		return "REGISTRO" + separator + "MATRICULA" + separator + "TIPO_ATS" + separator + "REGRA_DE_VOO" + separator + "TYPO" + separator + "EOBT" + separator + "FROM" + separator + "ORIGEM" + separator + "DESTINO" + separator + "OPERADOR" + separator + "NOME_CMTE" + separator + "ANAC_CMTE" + separator + "NOME_COPILOTO" + separator + "ANAC_COPILOTO" + separator + "TELEFONE" + separator + "DATA_AP_01" + separator + "HORA_AP" + separator + "DATA_HORA_AP" + separator + "DATA_HORA_TXT";
	}
	
	private String regAircraft 	 				= ""; 				// Registro da aeronave  .:. PPYSS
	private String msgType 	 				    = ""; 				// Tipos de mensagems FPL, Notivicacao, DLA, CHG e CNL   NO ( nao informado )
	private String operation 	 				= ""; 				// Tipos da operacao  IFR, Visual, Y ou Z    NO ( nao informado )
	private String hourEOBT    	 				= ""; 				// EOBT do FPL .:. 1823 (HHMM)
	private String fromValue    				= ""; 				// From da operacao para efeito de mensagem ou um indicativo AOCI  ( SBSP ) ou grupo ZZZZ
	private String orignValue    				= ""; 				// Origem do FPL .:. SBGR
	private String destValue     				= ""; 				// Destino do FPL .:. SBPA
	private String typeAircraft	 				= ""; 				// A109, B350 OU outro typo informado
	private String operatorOfAircfraft			= ""; 				// Operador da aeronave
	private String cmteName     				= ""; 				// Nome do piloto em comando
	private String cmteANAC				     	= ""; 				// ANAC do piloto em comando
	private String copName     					= ""; 				// Nome do Copiloto
	private String copANAC     					= ""; 				// ANAC do Copiloto
	private String phone     					= ""; 				// Numero do telefone para Ctt
	private String dateGroupPresentation     	= ""; 				// Data do FPL .:. 131213 (AAMMDD)  /  ID : 540      DATA/HORA : 13/12/13 - 23:15:06
	private String hourGroupPresentation     	= ""; 				// Data do FPL .:. 231506 (HHMMSS)  /  ID : 540      DATA/HORA : 13/12/13 - 23:15:06
	
	
	
	private String keyFPL        				= ""; 				// Chave destina ao confronto do FPL com a CONFAC  .:. PTDYE-SDTK-ZZZZ-SIAV-2712131950
	private String filename      				= ""; 				// Nome do arquivo
	private String fromC18Value  				= ""; 				// from Value  .:. SBSP
	private String depC18Value   				= ""; 				// from Value  .:. DEP/ BARRA DO GARCA, MT, 0255S04512W
	private String destC18Value  				= ""; 				// from Value  .:.DEST/ BARRA DO GARCA, MT, 0255S04512W
	private String dofGroup      				= ""; 				// DOF do plano de voo  DDMMAA
	
	private String groupDataHoraText = "";  		// FORMATO AAMMDDHHMM
	private String groupDataHoraBRS= "";  		// FORMATO DD/MM/AAAA HH:MM:SS
	
	
	public String getRegAircraft() {
		return regAircraft;
	}
	public void setRegAircraft(String regAircraft) {
		this.regAircraft = regAircraft;
	}
	public String getMsgType() {
		return msgType;
	}
	public void setMsgType(MsgTypeE msgType) {
		this.msgType = msgType.getValue();
	}
	public String getOperation() {
		return operation;
	}
	public void setOperation(OpE operation) {
		this.operation = operation.getValue();
	}
	
	public String getHourEOBT() {
		
		if( this.hourEOBT != null && this.hourEOBT.length() == 4 ){
			this.hourEOBT = this.hourEOBT.substring(0, 2) + ":" + this.hourEOBT.substring(2);
		}
		
		return this.hourEOBT;
	}
	
	public void setHourEOBT(String hourEOBT) {
		this.hourEOBT = hourEOBT;
	}
	public String getFromValue() {
		if( getMsgType().equals(MsgTypeE.CHG.getValue()) || 
			getMsgType().equals(MsgTypeE.CNL.getValue()) || 
			getMsgType().equals(MsgTypeE.DLA.getValue())   ){
			
			return "";
		}else{
			return fromValue;
		}
		
	}
	public void setFromValue(String fromValue) {
		this.fromValue = fromValue;
	}
	public String getOrignValue() {
		return orignValue;
	}
	public void setOrignValue(String orignValue) {
		this.orignValue = orignValue;
	}
	public String getDestValue() {
		return destValue;
	}
	public void setDestValue(String destValue) {
		this.destValue = destValue;
	}
	public String getTypeAircraft() {
		return typeAircraft;
	}
	public void setTypeAircraft(String typeAircraft) {
		this.typeAircraft = typeAircraft;
	}
	public String getOperatorOfAircfraft() {
		return operatorOfAircfraft;
	}
	public void setOperatorOfAircfraft(String operatorOfAircfraft) {
		this.operatorOfAircfraft = operatorOfAircfraft;
	}
	
	public String getCmteName() {
		return cmteName;
	}
	public void setCmteName(String cmteName) {
		this.cmteName = cmteName;
	}
	public String getCmteANAC() {
		return cmteANAC;
	}
	public void setCmteANAC(String cmteANAC) {
		this.cmteANAC = cmteANAC;
	}
	public String getCopName() {
		return copName;
	}
	public void setCopName(String copName) {
		this.copName = copName;
	}
	public String getCopANAC() {
		return copANAC;
	}
	public void setCopANAC(String copANAC) {
		this.copANAC = copANAC;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public String getDateGroupPresentation() {
		return dateGroupPresentation;
	}
	public void setDateGroupPresentation(String dateGroupPresentation) {
		this.dateGroupPresentation = dateGroupPresentation;
	}
	public String getHourGroupPresentation() {
		return hourGroupPresentation;
	}
	public void setHourGroupPresentation(String hourGroupPresentation) {
		this.hourGroupPresentation = hourGroupPresentation;
	}	
	public String getKeyFPL() {
		return keyFPL;
	}
	public void setKeyFPL(String keyFPL) {
		this.keyFPL = keyFPL;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getFromC18Value() {
		return fromC18Value;
	}
	public void setFromC18Value(String fromC18Value) {
		this.fromC18Value = fromC18Value;
	}
	public String getDepC18Value() {
		return depC18Value;
	}
	public void setDepC18Value(String depC18Value) {
		this.depC18Value = depC18Value;
	}
	public String getDestC18Value() {
		return destC18Value;
	}
	public void setDestC18Value(String destC18Value) {
		this.destC18Value = destC18Value;
	}
	public String getDofGroup() {
		return dofGroup;
	}
	public void setDofGroup(String dofGroup) {
		this.dofGroup = dofGroup;
	}
	
	
	/**
	 * Retorna o DataHora no formado de texto [AAMMDDHHMM]
	 * @return
	 */
	public String getGroupDataHoraText() {
		String tmpDate = getDateGroupPresentation();
		tmpDate = (tmpDate.substring(4) + tmpDate.substring(2, 4) + tmpDate.substring(0, 2) );
		
		String tmpHour = getHourGroupPresentation().replaceAll(":", "");
		this.groupDataHoraText = tmpDate + tmpHour;
		
		return this.groupDataHoraText;
	}
	
	/**
	 * Retorna o data hora no formato // FORMATO DD/MM/AAAA HH:MM:SS
	 * @return
	 */
	public String getGroupDataHoraBRS() {
		String tmpDate = getDateGroupPresentation();
		tmpDate = (tmpDate.substring(0,2) + "/" + tmpDate.substring(2, 4) + "/20" + tmpDate.substring(4) + " " );
		
		this.groupDataHoraBRS = tmpDate + getHourGroupPresentation();
		
		return this.groupDataHoraBRS;
	}
	
	
	
	/**
	 * Este metodo retorna uma String no formato de separado por virgula para cos campos da mensagem tratada
	 * os campos que nao possem dados ficarao vazios
	 * @param separator o separador fornecido pelo cliente "," ":" ou outro de interesse
	 * @return  um String sequandia conforma descrita abaixo  .:. RELATORIO SIMPLES
	 * "[NUMERO SEQUENCIAL DA MSG] [TIPO DE MENSAGEM -> FPL NTV CHG DLA CNL] [REGRA DE VOO -> IVYZ] [TYPO DA AERONAVE] [EOBT] [FROM] [ORIGEM] [DESTINO] [DATA DA APRESENTACAO -> DDMMAA] [HORA DA APRESENTACAO -> HH:MM:SS] [DATA HORA DA APRESENTACAO -> DD/MM/AAAA HH:MM:SS] [DATA HORA DA APRESENTACAO -> AAMMDDHHMMSS]"
	 * 0010157,PTHVU,NTV,V,B06,1130,SBMT,SBMT,SBMT,021213,11:15:15,131202111515
	 */
	public String getLineRegisterSimple(String separator) {
		return (getMsgType() + separator + getOperation() + separator + getTypeAircraft()  + separator + 
				getHourEOBT() + separator + getFromValue() + separator + getOrignValue() + separator + getDestValue() + separator +
				getDateGroupPresentation() + separator + getHourGroupPresentation() + separator + getGroupDataHoraBRS() + separator + getGroupDataHoraText());
	}
	
	/**
	 * Este metodo retorna uma String no formato de separado por virgula para cos campos da mensagem tratada
	 * os campos que nao possem dados ficarao vazios
	 * @param separator o separador fornecido pelo cliente "," ":" ou outro de interesse
	 * @return  um String sequenia conforma descrita abaixo .:. RELATORIO MODERADO
	 * "[NUMERO SEQUENCIAL DA MSG] [MATRICULA DA AERONAVE] [TIPO DE MENSAGEM -> FPL NTV CHG DLA CNL] [REGRA DE VOO -> IVYZ] [TYPO DA AERONAVE] [EOBT] [FROM] [ORIGEM] [DESTINO] [DATA DA APRESENTACAO -> DDMMAA] [HORA DA APRESENTACAO -> HH:MM:SS] [DATA HORA DA APRESENTACAO -> DD/MM/AAAA HH:MM:SS] [DATA HORA DA APRESENTACAO -> AAMMDDHHMMSS]"
	 * 0010157,NTV,V,B06,1130,SBMT,SBMT,SBMT,021213,11:15:15,131202111515
	 */
	public String getLineRegisterModerate(String separator) {
		return (getRegAircraft() + separator + getMsgType() + separator + getOperation() + separator + getTypeAircraft()  + separator + 
				getHourEOBT() + separator + getFromValue() + separator + getOrignValue() + separator + getDestValue() + separator +
				getDateGroupPresentation() + separator + getHourGroupPresentation() + separator + getGroupDataHoraBRS() + separator + getGroupDataHoraText());
	}
	
	/**
	 * Este metodo retorna uma String no formato de separado por virgula para cos campos da mensagem tratada
	 * os campos que nao possem dados ficarao vazios
	 * @param separator o separador fornecido pelo cliente "," ":" ou outro de interesse
	 * @return  um String sequandia conforma descrita abaixo .:. RELATORIO FULL
	 * "[NUMERO SEQUENCIAL DA MSG] [MATRICULA DA AERONAVE] [TIPO DE MENSAGEM -> FPL NTV CHG DLA CNL] [REGRA DE VOO -> IVYZ] [TYPO DA AERONAVE] [EOBT] [FROM] [ORIGEM] [DESTINO] [OPERADOR DA AERONAVE] [NOME CMTE] [ANAC CMTE] [NOME COPILOTO] [ANAC COPILOTO] [TELEFONE] [DATA DA APRESENTACAO -> DDMMAA] [HORA DA APRESENTACAO -> HH:MM:SS] [DATA HORA DA APRESENTACAO -> DD/MM/AAAA HH:MM:SS] [DATA HORA DA APRESENTACAO -> AAMMDDHHMMSS]"
	 * 0010157,PTHVU,NTV,V,B06,1130,SBMT,SBMT,SBMT,021213,11:15:15,131202111515
	 */
	public String getLineRegisterFull(String separator) {
		return (getRegAircraft() + separator + getMsgType() + separator + getOperation() + separator + getTypeAircraft()  + separator + 
				getHourEOBT() + separator + getFromValue() + separator + getOrignValue() + separator + getDestValue() + separator + getOperatorOfAircfraft() + separator +
				getCmteName() + separator + getCmteANAC() + separator + getCopName() + separator + getCopANAC() + separator + getPhone() + separator +
				getDateGroupPresentation() + separator + getHourGroupPresentation() + separator + getGroupDataHoraBRS() + separator + getGroupDataHoraText());
	}
	

	
	 /**
	  * Este metodo cria um grupo de String separando 
	  * os campo de ordenacao e mantendo a identidade da mensagem
	  * .:. 1312011525PPCOASBSJSBSR
	  * @return
	  */
	  public String getGroupFormOrdem(){
		return ( getGroupDataHoraText() + getRegAircraft() + getOrignValue() + getDestValue() );
	  }
	  
		
	@Override
	public int compareTo(ResumeMsg o) {
		return this.getGroupFormOrdem().compareTo(o.getGroupFormOrdem());
	}


	
}

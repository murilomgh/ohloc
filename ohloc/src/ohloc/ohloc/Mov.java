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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Representa um mensagem de MOV.
 * @author Jose Luiz
 *
 */
public class Mov implements  Comparable<Mov> {
	
	
	private static final String  REGEX_IS_VALID_CONFAC      		    = "[\\d]{0,7}\\(MOV-[0-9A-Z]{3,10}-[IVZY]-[A-Z]{4}-[A-Z]{4}-[A-Z]{4}-[0-9]{6}-[0-9]{10}\\)"; 	//  TO >>38833(MOV-PRRCE-V-SBMT-SBMT-SBMT-137821-0112131030)<<  Valida uma mensagem confac
	private static final Pattern PATTERN_IS_VALID_CONFAC 				= Pattern.compile(REGEX_IS_VALID_CONFAC);
	
	private String mov = "";					// MOV 38833(MOV-PRRCE-V-SBMT-SBMT-SBMT-137821-0112131030)
	private String matricula = "";				// Matricula da aeronave PPPXX
	private String ruleFly = ""; 				// Regra de voo  .:. IZVY
	private String groupFromOrgDest = ""; 		// Grupo SDPN-SBSJ-SBSR
	private String anac = ""; 					// código ANAC .:. 104116
	private String groupDataHora = "";  		// FORMATO AAMMDDHHMM
	
	public String getMov() {
		return mov;
	}
	public void setMov(String mov) {
		
		Matcher matcherCONFAC = PATTERN_IS_VALID_CONFAC.matcher(mov);
		
		if( matcherCONFAC.find() ){
			this.mov = mov;
		}else{
			this.mov = "00000(XXXXX-XXXXX-X-XXXX-XXXX-XXXX-000000-0000000000)";
		}
		
	}
	

	public String getMatricula() {
		String movTmp = this.mov;
		String matricula =  movTmp.substring((movTmp.indexOf("(MOV-") + 5), movTmp.length() - 36 ).trim();
		this.matricula = matricula;
		return this.matricula;
	}

	public String getRuleFly() {
		String movTmp = this.mov;
		String ruleFly =  movTmp.substring(movTmp.length() - 35 , movTmp.length() - 34).trim();
		this.ruleFly = ruleFly;
		return this.ruleFly;
	}

	public String getGroupFromOrgDest() {
		String movTmp = this.mov;
		String groupFromOrgDest =  movTmp.substring(movTmp.length() - 33 , movTmp.length() - 19).trim();
		this.groupFromOrgDest = groupFromOrgDest;
		return this.groupFromOrgDest;
	}

	public String getAnac() {
		String movTmp = this.mov;
		String anac =  movTmp.substring(movTmp.length() - 18 , movTmp.length() - 12).trim();
		this.anac = anac;
		return this.anac;
	}
	
	/**
	 * Retorna o DataHora no formado AAMMDDHHMM
	 * @return
	 */
	public String getGroupDataHora() {
		String movTmp = this.mov;
		String dh =  movTmp.substring(movTmp.length() - 11 , movTmp.length() - 1).trim();
		dh =  (dh.substring(4, 6) + dh.substring(2, 4) + dh.substring(0, 2) + dh.substring(6) );
		this.groupDataHora = dh;
		return this.groupDataHora;
	}
	
 /**
  * Este metodo cria um grupo de String separando 
  * os campo de ordenacao e mantendo a identidade da MOV
  * para o dia do movimento.  .:. PPCOA1312010900ISDPN-SBSJ-SBSR		 para a MOV (MOV-PPCOA-I-SDPN-SBSJ-SBSR-104116-0112130900)
  * @return
  */
	public String getGroupFormOrdem(){
		//return ( getMatricula() + getGroupDataHora() + getRuleFly() + getGroupFromOrgDest() + getAnac() );
		//return ( getMatricula() + getGroupDataHora() + getRuleFly() + getGroupFromOrgDest() );
		return ( getGroupDataHora() + getMatricula() + getRuleFly() + getGroupFromOrgDest() );
	}
	
	@Override
	public String toString() {
		return "Mov [matricula=" + getMatricula() + ", ruleFly="
				+ getRuleFly() + ", groupFromOrgDest=" + getGroupFromOrgDest()
				+ ", anac=" + getAnac() + ", groupDataHora=" + getGroupDataHora() + "ordem:" + getGroupFormOrdem() + "]";
	}
	
	
	@Override
	public int compareTo(Mov o) {
		return this.getGroupFormOrdem().compareTo(o.getGroupFormOrdem());
	}


}

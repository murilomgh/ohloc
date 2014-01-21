/**
 * 
 * Copyright 2014 Murilo Galvao Honorio ( murilo.honorio@gmail.com )
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
 * Representa um mensagem ISE.
 * @author Jose Luiz
 *
 */
public class Ise implements  Comparable<Ise> {
	
	
	private static final String  REGEX_IS_VALID_ISE      		    = "[\\d]{0,5}\\(ISE-[0-9A-Z]{5,9}-[A-Z]{4}-[INRX]-[0-9]{10}\\)"; 	//  TO >>122(ISE-PRRCE-SBSP-R-1701141230)<<  Valida uma mensagem ISE
	private static final Pattern PATTERN_IS_VALID_ISE 				= Pattern.compile(REGEX_IS_VALID_ISE);
	
	private String ise = "";					// ISE 122(ISE-PRRCE-SBSP-R-1701141230)
	private String matricula = "";				// Matricula da aeronave PPPXX
	private String tipoIsencao = ""; 			// Tipo de Isenção  .:. I N R X
	private String partida = ""; 				// Indicador de localidade SBSP
	private String groupDataHora = "";  		// FORMATO AAMMDDHHMM
	
	public String getIse() {
		return ise;
	}
	
	public void setIse(String ise) {
		
		Matcher matcherISE = PATTERN_IS_VALID_ISE.matcher(ise);
		
		if( matcherISE.find() ){
			this.ise = ise;
		}else{
			this.ise = "00000(XXXXX-XXXXX-XXXX-X-0000000000)";
		}
		
	}

	public String getMatricula() {
		String iseTmp = this.ise;
		String matricula =  iseTmp.substring((iseTmp.indexOf("(ISE-") + 5), iseTmp.length() - 19 ).trim();
		this.matricula = matricula;
		return this.matricula;
	}

	public String getTipoIsencao() {
		String iseTmp = this.ise;
		String tipoISE =  iseTmp.substring(iseTmp.length() - 13 , iseTmp.length() - 12).trim();
		this.tipoIsencao = tipoISE;
		return this.tipoIsencao;
	}

	public String getPartida() {
		String iseTmp = this.ise;
		String partida =  iseTmp.substring(iseTmp.length() - 18 , iseTmp.length() - 14).trim();
		this.partida = partida;
		return this.partida;
	}
	
	/**
	 * Retorna o DataHora no formado AAMMDDHHMM
	 * @return
	 */
	public String getGroupDataHora() {
		String iseTmp = this.ise;
		String dh =  iseTmp.substring(iseTmp.length() - 11 , iseTmp.length() - 1).trim();
		dh =  (dh.substring(4, 6) + dh.substring(2, 4) + dh.substring(0, 2) + dh.substring(6) );
		this.groupDataHora = dh;
		return this.groupDataHora;
	}
	
 /**
  * Este metodo cria um grupo de String separando os campo de ordenacao e mantendo a identidade da ISE
  * para o dia do movimento.  .:. PPCOA1312010900XSDPN para a ISE (ISE-PPCOA-X-SDPN-0112130900)
  * @return
  */
	public String getGroupFormOrdem(){
		//return ( getMatricula() + getGroupDataHora() + getRuleFly() + getGroupFromOrgDest() + getAnac() );
		//return ( getMatricula() + getGroupDataHora() + getRuleFly() + getGroupFromOrgDest() );
		return ( getGroupDataHora() + getMatricula() + getTipoIsencao() + getPartida() );
	}
	
	@Override
	public String toString() {
		return "Ise [matricula=" + getMatricula() + ", tipoIsencao="
				+ getTipoIsencao() + ", partida=" + getPartida()
				+ ", groupDataHora=" + getGroupDataHora() + "ordem:" + getGroupFormOrdem() + "]";
	}
	
	@Override
	public int compareTo(Ise o) {
		return this.getGroupFormOrdem().compareTo(o.getGroupFormOrdem());
	}
}

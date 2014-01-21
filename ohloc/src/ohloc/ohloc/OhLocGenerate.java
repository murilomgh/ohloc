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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohloc.ohloc.ResumeMsg.MsgTypeE;
import ohloc.ohloc.ResumeMsg.OpE;



/**
 * Este é o principal arquivo do sistema e entao representativo de sua 
 * funcionalidade efetua a leitura de todos os arquivos de plano de vôo 
 * e notificação de vôo da pasta FPL gerada pelo SAIS e então seleciona aquelas 
 * que possuem grupos ZZZZ no corpo da mensagem, em seguida ele faz a leitura 
 * das mensagens CONFAC inseridas na pasta CONFAC do sistema OHLOC v1.0 em busca 
 * das mensagens que necessitam de gerar LOC, assim ele cruza as informações e gera 
 * um relatório no formato de fechamento de serviço com todas as MOV ajustadas 
 * em numeração, ordenadas por matricula e EOBT, sem repetições e separada em 
 * blocos de 25 mensagens, seguidas pelos blocos de mensagens LOC separadas 
 * em grupo de 25 mensagens posicionadas no final do relatório gerado.
 * 
 * @author Jose Luiz
 *
 *
 * Foi feita inclusao de codigo referente as mensagens de isencao tarifaria (ISE) em 17/01/2014
 */
public class OhLocGenerate {
	
	
	public enum Log {
		DEBUG, INFO, ERROR, FATAL;  // Tipo de log do sistema
    }

	
	//Set de endereco de arquivo de configuracao
	//private static final String ADDRESS_FILE_FOR_CONFIG                 = "/config.properties";
	private static final String ADDRESS_FILE_FOR_CONFIG                 = "/config-complete.properties";
	
	

	/**
	 * Dica para uso de ferramenta para EXPRESSOES REGULARES
	 * http://www.gskinner.com/RegExr/	
	 * 
	 * Tutorial
	 * http://docs.oracle.com/javase/tutorial/essential/regex/index.html
	 * 
	 * java.util.regex.Pattern
	 * http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
	 * 
	 */
	
	
	private static final String  REGEX_IS_VALID_CONFAC      		    = "[\\d]{0,7}\\(MOV-[0-9A-Z]{3,10}-[IVZY]-[A-Z]{4}-[A-Z]{4}-[A-Z]{4}-[0-9]{6}-[0-9]{10}\\)"; 	//  TO >>38833(MOV-PRRCE-V-SBMT-SBMT-SBMT-137821-0112131030)<<  Valida uma mensagem confac
	private static final String  REGEX_IS_VALID_ISE						= "[\\d]{0,5}\\(ISE-[0-9A-Z]{5,9}-[A-Z]{4}-[INRX]-[0-9]{10}\\)";// //  TO >>122(ISE-PRRCE-SBSP-R-1701141230)<<  Valida uma mensagem ISE
	private static final String  REGEX_IS_VALID_CONFAC_OR_MALFORMED	    = "[\\d]{0,7}\\(MOV-.{0,100}\\)"; 								// Este REGEX baliza uma mensagem CONFAC mal formada, ou seja, se a linha nao for uma CONFAC valida e passar por este crivo ele deve ser uma msg CONFAC mal formada
	private static final String  REGEX_CONFAC_ZZZZ_GROUP        		= "ZZZZ"; 														// ZZZZ  		TO 38833(MOV- PRRCE-V-SBMT->>ZZZZ<<-SBMT-137821-0112131030)  Encontrar um grupo ZZZZ na CONFA
	private static final String  REGEX_FPL_IS_REG_AIRCRAFT  		    = "\\(FPL-[0-9A-Z]{3,10}"; 										//  TO (FPL-PRWBA-VG   localiza o registro da aeronave
	private static final String  REGEX_FPL_VALIDADE_REG_AIRCRAFT  		= "^[0-9A-Z]{3,10}$"; 											//  TO [PRWBA] Valida uma matricula de aeronave
	private static final String  REGEX_FPL_IS_MSG_TYPE_FPL  		    = "^\\(FPL-[0-9A-Z]{3,10}-[A-Z]{2}$"; 							//  TO (FPL-PRWBA-VG   localiza uma mensagem de plano de voo completo
	private static final String  REGEX_FPL_IS_MSG_TYPE_FPLS  		    = "^\\(FPL-[0-9A-Z]{3,10}$|^\\(FPL-[0-9A-Z]{3,10}-[A-Z]{1}$"; 									//  TO (FPL-PRWBA      localiza uma mensagem de notificacao de voo
	private static final String  REGEX_FPL_IS_MSG_TYPE_CHG  		    = "^\\(CHG-[0-9A-Z]{3,10}-[A-Z]{4}"; 							//  TO [(CHG-GLO1204-SBSP] -420--BPA-0-13/SBSP1410)   localiza uma mensagem de CHG
	private static final String  REGEX_FPL_IS_MSG_TYPE_DLA  		    = "^\\(DLA-[0-9A-Z]{3,10}-";									//  TO [(DLA-PRFJO-]   SBMT1430-SDIH-0)  localiza uma mensagem de DLA
	private static final String  REGEX_FPL_IS_MSG_TYPE_CNL  		    = "^\\(CNL-[0-9A-Z]{3,10}-[A-Z]{4}"; 							//  TO [(CNL-PTYTE-SBMT]   -SBMT-0)  localiza uma mensagem de CNL
	private static final String  REGEX_FPL_IS_MSG_TYPE_FPL_OP_IFR  		= "^\\(FPL-[0-9A-Z]{3,10}-I[A-Z]{1}$"; 							//  TO (FPL-PRWBA-IG   localiza uma mensagem de plano de voo completo IFR
	private static final String  REGEX_FPL_IS_MSG_TYPE_FPL_OP_VFR  		= "^\\(FPL-[0-9A-Z]{3,10}-V[A-Z]{1}$"; 							//  TO (FPL-PRWBA-VG   localiza uma mensagem de plano de voo completo VFR
	private static final String  REGEX_FPL_IS_MSG_TYPE_FPL_OP_Y  		= "^\\(FPL-[0-9A-Z]{3,10}-Y[A-Z]{1}$"; 							//  TO (FPL-PRWBA-YG   localiza uma mensagem de plano de voo completo Y
	private static final String  REGEX_FPL_IS_MSG_TYPE_FPL_OP_Z  		= "^\\(FPL-[0-9A-Z]{3,10}-Z[A-Z]{1}$"; 							//  TO (FPL-PRWBA-ZG   localiza uma mensagem de plano de voo completo Z
	private static final String  REGEX_FPL_IS_FROM      		        = "^[A-Z]{4}$";; 		                                        //  TO  designativo OACI de 4 caracteres ou ZZZZ
	private static final String  REGEX_FPL_IS_AIRCFRT_TYPE	 		     = "^-[0-9A-Z]{3,4}/[A-Z]{1}-|^-[0-9A-Z]{3,4}-[A-Z]{1}";; 		                            //  TO  localizar o typo da aeronave .:. A19S
	private static final String  REGEX_FPL_IS_ORIGN      		        = "^-[A-Z]{4}[0-9]{4}$"; 										//  TO -SBSP1750   Informa a origem no FPL junto com o grupo data hora de partida
	private static final String  REGEX_FPL_IS_DEST      		        = "-[A-Z]{4}[0-9]{4} [A-Z]{4}"; 								//  TO -SDSL0010 SIBH   Informa o destino do FPL
	private static final String  REGEX_FPL_IS_FROM_C18      		    = "RMK[ ]{0,64}/[ ]{0,64}FROM | FROM[ ]{0,64}/[ ]{0,64}"; 		//  TO   RMK/ FROM SBSP Localiza o FROM
	private static final String  REGEX_FPL_IS_DEP_C18      		        = "DEP[ ]{0,64}/[ ]{0,64}"; 									//  TO   DEP/ BARRA DO GARCA, MT, 0255S04512W Localiza o DEP
	private static final String  REGEX_FPL_IS_DEST_C18      		    = "DEST[ ]{0,64}/[ ]{0,64}"; 									//  TO   DEST/ BARRA DO GARCA, MT, 0255S04512W  Localiza o DEST
	private static final String  REGEX_FPL_DATE_HOUR      		        = "[0-9]{2}/[0-9]{2}/[0-9]{2} - [0-9]{2}:[0-9]{2}:[0-9]{2}$"; 	//  TO 26/12/13 - 09:50:25   localiza a data hora do fpl
	private static final String  REGEX_FPL_KEY_INDEX      		        = "[0-9A-Z]{3,10}-[0-9]{10}"; 									//  TO PPVVV-1312041200   chave que consta da origem, destino e datahora eobt
	private static final String  REGEX_FPL_IS_DOF      		            = "DOF[ ]{0,64}/[ ]{0,64}[0-9]{6}"; 							//  TO -DOF/131214    Localiza o DOF
	private static final String  REGEX_FPL_IS_NAME_FILE_CONFAC          = "confac_[0-9]{6}.[a-z]{3}"; 									//  TO CONFAC_031213.TXT    Nome de arquivo CONFAC >> LOWER CASE
	private static final String  REGEX_IS_DATE         				 	= "^[0-9]{6}$"; 									            //  TO DATE .:. 020414
	private static final String  REGEX_IS_HOUR         					= "^[0-1]{1}[0-9]{1}:[0-5]{1}[0-9]{1}:[0-5]{1}[0-9]{1}$|^[0-2]{1}[0-3]{1}:[0-5]{1}[0-9]{1}:[0-5]{1}[0-9]{1}$"; 	//  TO HORA .:. 23:15:35
	private static final String  REGEX_FPL_IS_END_MSG  					= "\\)"; 
	private static final String  REGEX_OPERATOR 						= "OPR[ ]{0,64}/[ ]{0,64}"; 									// OPR/ -  TO  OPR/GUSTAVO FIGUEIREDOR      ENCONTRADO NA MENSAGEM ex: ... 0 SBSJ -OPR/GUSTAVO FIGUEIREDORMK/FROM ...
	private static final String  REGEX_NAME_ANAC_CMTE 					= "C/[A-Z ]{0,30}[0-9]{6}"; 									// C/ -  TO   [C/MARCELO  107837])
	private static final String  REGEX_NAME_ANAC_PHONE_COP 				= "> PREENCHIDO POR"; 										// TO   [===> PREENCHIDO POR]: MARCELO 107837 ===> TELCTT : 21 9 8167 1911  .:. Linha com Nome, ANAC e phone
	private static final String  REGEX_NAME 							= "[A-Z ]{2,30}"; 									            // C/ -  TO   C/[MARCELO SOUZA] 107837)  - SOMENTE PARA PESQUISA NA LINHA ALVO
	private static final String  REGEX_ANAC 							= "[0-9]{6}"; 													// C/ -  TO   C/MARCELO SOUZA [107837])  - SOMENTE PARA PESQUISA NA LINHA ALVO
	private static final String  REGEX_PHONE_LINE    					= "TELCTT[: ]{0,10} [0-9-. ]{8,20}"; 							// TO   [===> PREENCHIDO POR]: MARCELO 107837 ===> [TELCTT [21 9 8167 1911]  .:. - SOMENTE PARA PESQUISA NA LINHA ALVO
	private static final String  REGEX_PHONE 							= "[0-9-. ]{8,20}"; 											// TO   [===> PREENCHIDO POR]: MARCELO 107837 ===> TELCTT : [21 9 8167 1911]  .:. - SOMENTE PARA PESQUISA NA LINHA ALVO
	private static final String  REGEX_ORGN 							= "[A-Z]{4}"; 	 												// Orifinador .:. SOMENTE PARA PESQUISA NA LINHA ALVO
	private static final String  REGEX_EOBT 							= "[0-9]{4}";  													// EOBT FPL .:. SOMENTE PARA PESQUISA NA LINHA ALVO

	private static final Pattern PATTERN_FPL_IS_REG_AIRCRAFT			= Pattern.compile(REGEX_FPL_IS_REG_AIRCRAFT);
	private static final Pattern PATTERN_FPL_VALIDADE_REG_AIRCRAFT		= Pattern.compile(REGEX_FPL_VALIDADE_REG_AIRCRAFT);
	private static final Pattern PATTERN_FPL_IS_MSG_TYPE_FPL			= Pattern.compile(REGEX_FPL_IS_MSG_TYPE_FPL);
	private static final Pattern PATTERN_FPL_IS_MSG_TYPE_FPLS			= Pattern.compile(REGEX_FPL_IS_MSG_TYPE_FPLS);
	private static final Pattern PATTERN_FPL_IS_MSG_TYPE_CHG			= Pattern.compile(REGEX_FPL_IS_MSG_TYPE_CHG);
	private static final Pattern PATTERN_FPL_IS_MSG_TYPE_DLA			= Pattern.compile(REGEX_FPL_IS_MSG_TYPE_DLA);
	private static final Pattern PATTERN_FPL_IS_MSG_TYPE_CNL			= Pattern.compile(REGEX_FPL_IS_MSG_TYPE_CNL);
	private static final Pattern PATTERN_FPL_IS_MSG_TYPE_FPL_OP_IFR		= Pattern.compile(REGEX_FPL_IS_MSG_TYPE_FPL_OP_IFR);
	private static final Pattern PATTERN_FPL_IS_MSG_TYPE_FPL_OP_VFR		= Pattern.compile(REGEX_FPL_IS_MSG_TYPE_FPL_OP_VFR);
	private static final Pattern PATTERN_FPL_IS_MSG_TYPE_FPL_OP_Y		= Pattern.compile(REGEX_FPL_IS_MSG_TYPE_FPL_OP_Y);
	private static final Pattern PATTERN_FPL_IS_MSG_TYPE_FPL_OP_Z 		= Pattern.compile(REGEX_FPL_IS_MSG_TYPE_FPL_OP_Z);
	private static final Pattern PATTERN_FPL_IS_FROM 					= Pattern.compile(REGEX_FPL_IS_FROM);
	private static final Pattern PATTERN_FPL_IS_AIRCFRT_TYPE 		    = Pattern.compile(REGEX_FPL_IS_AIRCFRT_TYPE);
	private static final Pattern PATTERN_FPL_IS_ORIGN 					= Pattern.compile(REGEX_FPL_IS_ORIGN);
	private static final Pattern PATTERN_FPL_IS_DEST 					= Pattern.compile(REGEX_FPL_IS_DEST);
	private static final Pattern PATTERN_FPL_IS_FROM_C18 				= Pattern.compile(REGEX_FPL_IS_FROM_C18);
	private static final Pattern PATTERN_FPL_IS_DEP_C18 				= Pattern.compile(REGEX_FPL_IS_DEP_C18);
	private static final Pattern PATTERN_FPL_IS_DEST_C18 				= Pattern.compile(REGEX_FPL_IS_DEST_C18);
	private static final Pattern PATTERN_FPL_DATE_HOUR 					= Pattern.compile(REGEX_FPL_DATE_HOUR);
	private static final Pattern PATTERN_FPL_KEY_INDEX 					= Pattern.compile(REGEX_FPL_KEY_INDEX);
	private static final Pattern PATTERN_FPL_IS_DOF 					= Pattern.compile(REGEX_FPL_IS_DOF);
	private static final Pattern PATTERN_FPL_IS_NAME_FILE_CONFAC     	= Pattern.compile(REGEX_FPL_IS_NAME_FILE_CONFAC);
	private static final Pattern PATTERN_IS_VALID_CONFAC 				= Pattern.compile(REGEX_IS_VALID_CONFAC);
	private static final Pattern PATTERN_IS_VALID_ISE					= Pattern.compile(REGEX_IS_VALID_ISE); //Pattern da ISE
	private static final Pattern PATTERN_IS_VALID_CONFAC_OR_MALFORMED 	= Pattern.compile(REGEX_IS_VALID_CONFAC_OR_MALFORMED);

	private static final Pattern PATTERN_CONFAC_ZZZZ_GROUP 				= Pattern.compile(REGEX_CONFAC_ZZZZ_GROUP);
	private static final Pattern PATTERN_IS_DATE 						= Pattern.compile(REGEX_IS_DATE);
	private static final Pattern PATTERN_IS_HOUR 						= Pattern.compile(REGEX_IS_HOUR);
	private static final Pattern PATTERN_FPL_IS_END_MSG 				= Pattern.compile(REGEX_FPL_IS_END_MSG);
	private static final Pattern PATTERN_OPERATOR						= Pattern.compile(REGEX_OPERATOR);
	private static final Pattern PATTERN_NAME_ANAC_CMTE					= Pattern.compile(REGEX_NAME_ANAC_CMTE);
	private static final Pattern PATTERN_NAME_ANAC_PHONE_COP		    = Pattern.compile(REGEX_NAME_ANAC_PHONE_COP);
	private static final Pattern PATTERN_NAME							= Pattern.compile(REGEX_NAME);
	private static final Pattern PATTERN_ANAC							= Pattern.compile(REGEX_ANAC);
	private static final Pattern PATTERN_PHONE_LINE	    				= Pattern.compile(REGEX_PHONE_LINE);
	private static final Pattern PATTERN_PHONE		    				= Pattern.compile(REGEX_PHONE);
	private static final Pattern PATTERN_ORGN		    				= Pattern.compile(REGEX_ORGN);
	private static final Pattern PATTERN_EOBT		    				= Pattern.compile(REGEX_EOBT);
	
	private static final String EXTENSION_FILE_TO_READ 				    =".txt"; // Extensao de arquivo permitido para leitura
	
	
	// Informa os enderecos para obtencao de arquivos e os valores padroes em caso de falha de leitura do arquivo  config.properties.
	private static String ADDRESS_FPL_OF_TARGET_FOLDER		= "C:\\sais\\FPL"; 					// Endereco dos arquivos onde estao armazenados os FPL .:. C:\\_tmp\\LocGenerator\\FPL
	private static String ADDRESS_CONFAC_OF_TARGET_FOLDER	= "C:\\sais\\CONFAC";				// Endereco dos arquivos onde estao armazenados as MSG CONFAC .:. C:\\_tmp\\LocGenerator\\CONFAC
	private static String ADDRESS_GENERATE_FILES_REPORT		= "C:\\sais\\RELATORIOS";			// Endereco onde sera armazenados os relatorios gerados .:. C:\\_tmp\\LocGenerator\\RELATORIOS
	
	// Entrada de texto a ser customizado e os valores padroes em caso de falha de leitura do arquivo  config.properties.
	private static String HEADER_OF_FILE					= "CENTRO AIS - SVC";  						//CENTRO AIS - SVC 01/12/2013
	private static String TITLE_NUMBER_OF_ISE				= "ISE _____________";  					//ISE MT __________
	private static String TITLE_NUMBER_OF_LOC				= "LOC _____________";  					//LOC _____________
	private static String TITLE_NUMBER_OF_MOV				= "MOV _____________";	  					//MOV _____________
	private static String TITLE_NUMBER_OF_MTE				= "MTE _____________";  					//MTE _____________
	private static String TITLE_MOV							= "LISTAGEM DE MENSAGENS MOV";  			//MOV
	private static String TITLE_LOC							= "LISTAGEM DE MENSAGENS LOC"; 				//LOC
	private static String TITLE_ISE							= "LISTAGEM DE MENSAGENS ISE (SAIS)";		//ISE
	private static String TITLE_HEADER_OF_BLOCKS_MOV		= "CONFAC - CENTRAL AIS-SP - DIA"; 			//CONFAC - CENTRAL AIS-SP - DIA
	private static String TITLE_HEADER_OF_BLOCKS_LOC		= "LOC - CENTRAL AIS-SP - DIA";  			//LOC - CENTRAL AIS-SP - DIA
	private static String TITLE_HEADER_OF_BLOCKS_ISE		= "ISE - CENTRAL AIS-SP - DIA";  			//ISE - CENTRAL AIS-SP - DIA
	private static String TITLE_FILE_NAME_GENERATE			= "RELATORIO_VIA_";  						//prefixo para nome de arquivo gerado
	private static String TITLE_REPORT_FOR_STATISTICS		= "RELATORIO_PARA_FINS_ESTATISTICO_";   	//prefixo para gerar relarorio estatistico
	
	//Variaveis para customizacao e os valores padroes em caso de falha de leitura do arquivo config.properties.
	private static int NUMBER_OF_MSG_FOR_BLOCK_MOV			= 25;  							  			// Numero de mensagens por bloco para MOV
	private static int NUMBER_OF_MSG_FOR_BLOCK_LOC			= 25;  										// Numero de mensagens por bloco para LOC
	private static int NUMBER_OF_MSG_FOR_BLOCK_ISE			= 25;  										// Numero de mensagens por bloco para ISE
	private static int IS_NUMERATION_FOR_ISE				= 0;										// 1: ISE com numeracao  0: ISE sem numeracao
	private static int IS_GENERATE_REPORTY_FOR_STATISTICS	= 0; 										// 1: Gera relarotio para base estatistica  0: Nao gera relatorio
	private static String TYPE_FOR_REPORT_STATICS	 	    = ResumeMsg.ReportTypeE.SIMPLE.getValue();	// Tipo de relatorio estatistico gerado   S - Relatorio Simples   M - Relatorio Moderado   F - Relatorio Completo
	private static int IS_READ_FILES_ALL_DIRECTORY_LEVEL	= 0; 										// 1: Sera efetuada leitura de arquivos de subpastas  0: Somente sera feito leitura da arquivos da pasta de primeiro nivel
	private static String SEPARATOR_FOR_REPORT_STATICS	 	= ",";										// Separador utilizado para separar os campo no relatorio para estatistica  (,)
	
	 
	
	private static DecimalFormat dfCountNumber;
	private static DecimalFormat dfCountG;
	private static DecimalFormat dfCountM;
	private static DecimalFormat dfCountC;
	private static DecimalFormat dfEOBT;
	private static DecimalFormat dfPercent;
	private static SimpleDateFormat ft;
	private static SimpleDateFormat ftDateFile;
	
	
	
	private static String LINE_SEPARATOR = System.getProperty("line.separator");
	
	private static Properties props;
	
	
	public static void main(String[] args) {
		
		dfCountNumber  = new DecimalFormat("####"); 			// 8 digitos com zero absent
		dfCountG  = new DecimalFormat("0000000"); 				// 7 digitos obrigatorios
    	dfCountM  = new DecimalFormat("0000"); 					// 4 digitos obrigatorios
    	dfCountC  = new DecimalFormat("000");  					// 3 digitos obrigatorios
    	dfEOBT  = new DecimalFormat("0000");  					// 4 digitos obrigatorios  0501
    	dfPercent = new DecimalFormat("0.00");                  // percentual
        ft =new SimpleDateFormat ("hh:mm:ss,SSS");  			// 09:30:21,599
        ftDateFile =new SimpleDateFormat ("yyyy_MM_dd_hh_mm_ss_SSS");  			// 2014_01_05_10_39_29_361
    	
		props = new Properties(); 
		try {
			props.load((OhLocGenerate.class.getResourceAsStream(ADDRESS_FILE_FOR_CONFIG)));
			
			
			// Informa os enderecos para obtencao de arquivos
			ADDRESS_FPL_OF_TARGET_FOLDER 		= ( props.getProperty("adress-for-fpl-repository") == null ? ADDRESS_FPL_OF_TARGET_FOLDER : props.getProperty("adress-for-fpl-repository").trim() );
			ADDRESS_CONFAC_OF_TARGET_FOLDER 	= ( props.getProperty("adress-for-confac-repository") == null ? ADDRESS_CONFAC_OF_TARGET_FOLDER : props.getProperty("adress-for-confac-repository").trim() );
			ADDRESS_GENERATE_FILES_REPORT 		= ( props.getProperty("adress-generate-report-files") == null ? ADDRESS_GENERATE_FILES_REPORT : props.getProperty("adress-generate-report-files").trim() );
			
			// Entrada de texto a ser customizado
			HEADER_OF_FILE						= ( props.getProperty("msg-header-of-file") == null ? HEADER_OF_FILE : props.getProperty("msg-header-of-file").trim() );
			TITLE_NUMBER_OF_ISE 				= ( props.getProperty("msg-title-number-of-ise") == null ? TITLE_NUMBER_OF_ISE : props.getProperty("msg-title-number-of-ise").trim() );
			TITLE_NUMBER_OF_LOC 				= ( props.getProperty("msg-title-number-of-loc") == null ? TITLE_NUMBER_OF_LOC : props.getProperty("msg-title-number-of-loc").trim() );
			TITLE_NUMBER_OF_MOV 				= ( props.getProperty("msg-title-number-of-mov") == null ? TITLE_NUMBER_OF_MOV : props.getProperty("msg-title-number-of-mov").trim() );
			TITLE_NUMBER_OF_MTE 				= ( props.getProperty("msg-title-number-of-mte") == null ? TITLE_NUMBER_OF_MTE : props.getProperty("msg-title-number-of-mte").trim() );
			TITLE_MOV 							= ( props.getProperty("msg-title-of-mov") == null ? TITLE_MOV : props.getProperty("msg-title-of-mov").trim() );
			TITLE_LOC 							= ( props.getProperty("msg-title-of-loc") == null ? TITLE_LOC : props.getProperty("msg-title-of-loc").trim() );
			TITLE_ISE 							= ( props.getProperty("msg-title-of-ise") == null ? TITLE_ISE : props.getProperty("msg-title-of-ise").trim() );
			TITLE_HEADER_OF_BLOCKS_MOV			= ( props.getProperty("msg-title-header-of-blocks-mov") == null ? TITLE_HEADER_OF_BLOCKS_MOV : props.getProperty("msg-title-header-of-blocks-mov").trim() );
			TITLE_HEADER_OF_BLOCKS_LOC 			= ( props.getProperty("msg-title-header-of-blocks-loc") == null ? TITLE_HEADER_OF_BLOCKS_LOC : props.getProperty("msg-title-header-of-blocks-loc").trim() );
			TITLE_HEADER_OF_BLOCKS_ISE 			= ( props.getProperty("msg-title-header-of-blocks-ise") == null ? TITLE_HEADER_OF_BLOCKS_ISE : props.getProperty("msg-title-header-of-blocks-ise").trim() );
			TITLE_FILE_NAME_GENERATE 			= ( props.getProperty("msg-title-file-name-generate") == null ? TITLE_FILE_NAME_GENERATE : props.getProperty("msg-title-file-name-generate").trim() );
			TITLE_REPORT_FOR_STATISTICS         = ( props.getProperty("title-report-for-statistics") == null ? TITLE_REPORT_FOR_STATISTICS : props.getProperty("title-report-for-statistics").trim() );
			
			//System.out.println("title-report-for-statistics: " + TITLE_REPORT_FOR_STATISTICS);
			
			
			//Variaveis para customizacao 
			NUMBER_OF_MSG_FOR_BLOCK_MOV 		= Integer.parseInt( (props.getProperty("number-of-msg-for-block-mov") == null ? String.valueOf(NUMBER_OF_MSG_FOR_BLOCK_MOV) : props.getProperty("number-of-msg-for-block-mov").trim()) );
			NUMBER_OF_MSG_FOR_BLOCK_LOC 		= Integer.parseInt( (props.getProperty("number-of-msg-for-block-loc") == null ? String.valueOf(NUMBER_OF_MSG_FOR_BLOCK_LOC) : props.getProperty("number-of-msg-for-block-loc").trim()) );
			NUMBER_OF_MSG_FOR_BLOCK_ISE 		= Integer.parseInt( (props.getProperty("number-of-msg-for-block-ise") == null ? String.valueOf(NUMBER_OF_MSG_FOR_BLOCK_ISE) : props.getProperty("number-of-msg-for-block-ise").trim()) );
			IS_NUMERATION_FOR_ISE				= Integer.parseInt( (props.getProperty("number-of-msg-for-block-ise") == null ? String.valueOf(IS_NUMERATION_FOR_ISE) : props.getProperty("is-numeration-for-ise").trim()) );
			IS_GENERATE_REPORTY_FOR_STATISTICS  = Integer.parseInt( (props.getProperty("is-generate-report-for-statistics") == null ? String.valueOf(IS_GENERATE_REPORTY_FOR_STATISTICS) : props.getProperty("is-generate-report-for-statistics").trim()) );
			TYPE_FOR_REPORT_STATICS        		= ( props.getProperty("type-for-report-statistics") == null ? TYPE_FOR_REPORT_STATICS : props.getProperty("type-for-report-statistics").trim() );
			IS_READ_FILES_ALL_DIRECTORY_LEVEL   = Integer.parseInt( (props.getProperty("is-read-files-all-directory-level") == null ? String.valueOf(IS_READ_FILES_ALL_DIRECTORY_LEVEL) : props.getProperty("is-read-files-all-directory-level").trim()) );
			SEPARATOR_FOR_REPORT_STATICS        = ( props.getProperty("separator-for-report-statistics") == null ? SEPARATOR_FOR_REPORT_STATICS : props.getProperty("separator-for-report-statistics").trim() );
			
			
		} catch (IOException e1) {
			System.out.println( getLog(Log.ERROR) + "Arquivo config.properties NAO encontrado." + e1.getMessage());
			//e1.printStackTrace();
		}
    	
    	
// FASE 1 - CRIAR INDICE DE ARQUIVOS DE MENSGENS ATS PARA PESQUISA
		
    			HashMap<String, ResumeMsg> mapFPLForLoc = new HashMap<String, ResumeMsg>();  // Mapeara todos os Dados Uteis dos FPL necessario para confeccao de LOC
    			TreeSet<ResumeMsg>  tSetOfResumeMsg = new TreeSet<ResumeMsg>(); //Armazenara todas as mensagem FPL lidas para gerar um arquivo separado por virgula para estatistica
    			
    			ArrayList<String>  listOfFPLFiles = new ArrayList<String>(); 
    			    				
    			System.out.println("                                                                          " );
    			System.out.println("##########################################################################" );
    			System.out.println("#                                                                        #" );
    			System.out.println("# O Sistema OHLOC foi construido como uma forma de agradecimento         #" );
    			System.out.println("# aos meus colegas da CENTRAL AIS-SP. A estes excelentes profissionais   #" );
    			System.out.println("# so poderia deixar o meu Muito Obrigado pela camaradagem e amizade      #" ); 
    			System.out.println("# de muitos anos.                                                        #" );
    			System.out.println("#                                                                        #" );
    			System.out.println("#                               Jose Luiz da Silva - 2S SAI (01/01/2014) #" );
    			System.out.println("#                                                                        #" );
    			System.out.println("##########################################################################" );
    			System.out.println("                                                                          " );
    			System.out.println("Mais informacoes leia o arquivo: leiame.pdf" );
    			System.out.println("                                                                          " );
    			
    			System.out.println("-------------------------------------------------------------------------");
    			
    			System.out.println("                                                                          " );
    			
    			System.out.println( getLog(Log.INFO) + "Sistema OHLOC iniciado. Favor Aguarde..." );
    			
    			
    			
    			//Murilo: realizei correcao do filtro de CONFAC para ler apenas os .TXT, independente de estarem em minusculas ou maiusculas
    			//Da forma original, o programa estava copiando outros arquivos (.dll, .bat) da pasta CONFAC para a pasta RELATORIO
    			FilenameFilter filtro = new FilenameFilter() { // Isso é um inner Class.. Este é um Filtro para Leitura de arquivos
    				public boolean accept(File dir, String name) {
    					return name.toLowerCase().endsWith(EXTENSION_FILE_TO_READ); // aqui você coloca seu filtro... Neste caso todos os que termine com a extencao .txt
    				}
    			};
    			
    			
    			
    			
    			// EFETUA A LEITURA DOS ARQUIVOS DE MENSAGENS ATS .:. FPL FPLs CHG DLA CNL
    			if( IS_READ_FILES_ALL_DIRECTORY_LEVEL == 1 ){ // Para leitura de arquivos contidos em diretorios e subdiretorios
    				
	    			FileUtil fileUtil = new FileUtil(); 
	    	        List<File> files = fileUtil.listFiles(new File (ADDRESS_FPL_OF_TARGET_FOLDER), EXTENSION_FILE_TO_READ);  
	    	        
	    	        // OBTEM OS NOMES DOS ARQUIVOS
	    			for (File f : files) { // Obtem o nome de todos os arquivo da pasta ...\\CONFAC e subdiretorios
	    				listOfFPLFiles.add(f.getAbsolutePath());
	    			}
	    			
    			}else{  // Para leitura de arquivos no primeiro nível do diretorio
    				

	    			File diretorioFPL = new File(ADDRESS_FPL_OF_TARGET_FOLDER);   
	    			String [] arquivosFPL = diretorioFPL.list(filtro);
	    			
	    			// OBTEM OS NOMES DOS ARQUIVOS
	    			
	    			if(arquivosFPL == null){
	    				
	    				System.out.println( getLog(Log.FATAL) + "Endereco para pasta de arquivos FPL NAO encontrado, ou a pasta esta vazia. Acertar parametros do arquivo config.properties" );
		    			System.exit(0);
		    			
	    			}else{
	    				
		    			for(int i = 0; i < arquivosFPL.length; i++) { // Obtem o nome de todos os arquivo da pasta ...\\CONFAC
		    				
		    				if( arquivosFPL[i].toLowerCase().endsWith(EXTENSION_FILE_TO_READ)){
		    					listOfFPLFiles.add(ADDRESS_FPL_OF_TARGET_FOLDER + "\\" + arquivosFPL[i]);
		    				}
		    				
		    			}
	    				
	    			}
	    			
	    			
    			
    			}

    			
    			
    			
    			
    			System.out.println( getLog(Log.INFO) + "Lendo " + listOfFPLFiles.size() + " arquivo (s) da pasta " + ADDRESS_FPL_OF_TARGET_FOLDER + "..." );

    			// Contadores de mensagens para teste
    			int countFPL = 0;
    			int countFPLs = 0;
    			int countCHG = 0;
    			int countDLA = 0;
    			int countCNL = 0;
    			
    			
    			// PROCESSA CADA ARQUIVO INDIVIDUALMENTE
    			for (Iterator<String> iter = listOfFPLFiles.iterator(); iter.hasNext();) {	 // Efetua a leitura de cada um dos arquivos CONFAC para efetuar o processamento		
    				
	    			String fileNameFPL = iter.next(); // Nome do arquivo CONFAC que sera realizada a leitura
	    				
	    				ResumeMsg rMsgForReport = new ResumeMsg();
	    				
	    				BufferedReader br;
	    				try {
	    					
	    					String regAircraft   				= ""; 					// from Value  .:. SBSP
	    					MsgTypeE msgType 	 				= MsgTypeE.NO; 			// Tipos de mensagems FPL, Notivicacao, DLA, CHG e CNL   NO ( nao informado )
	    					OpE operation 	 	 				= OpE.NO; 			  	// Tipos da operacao  IFR, Visual, Y ou Z    NO ( nao informado )
	    					String hourEOBT      				= ""; 					// EOBT do FPL .:. 1823 (HHMM)
	    					String fromValue    				= ""; 					// From da operacao para efeito de mensagem ou um indicativo AOCI  ( SBSP ) ou grupo ZZZZ
	    					String orignValue 	 				= ""; 					// Origem do FPL .:. SBGR
	    					String destValue     				= ""; 					// Destino do FPL .:. SBPA
	    					String typeAircraft					= ""; 					// A109, B350 OU outro typo informado
	    					String cmteName     				= ""; 					// Nome do piloto em comando
	    					String cmteANAC				     	= ""; 					// ANAC do piloto em comando
	    					String copName     					= ""; 					// Nome do Copiloto
	    					String copANAC     					= ""; 					// ANAC do Copiloto
	    					String phone     					= ""; 					// Numero do telefone para Ctt
	    					String dateGroupPresentation     	= ""; 					// Data do FPL .:. 131213 (AAMMDD)  /  ID : 540      DATA/HORA : 13/12/13 - 23:15:06
	    					String dateGroupPresentationLOC 	= ""; 					// Para uso na geracao de LOC - KeyFPL; no caso da existencia de DOF ele teá preferencia ( Data do FPL .:. 131213 (AAMMDD)  /  ID : 540      DATA/HORA : 13/12/13 - 23:15:06  ou DOF )
	    					String hourGroupPresentation     	= ""; 					// Data do FPL .:. 231506 (HHMMSS)  /  ID : 540      DATA/HORA : 13/12/13 - 23:15:06
	    					String operatorOfAircfraft			= ""; 					// Operador da aeronave
	    					String keyFPL        				= ""; 					// Chave destina ao confronto do FPL com a CONFAC  .:. SDTK-ZZZZ-SIAV-2712131950
	    					String fromC18Value  				= ""; 					// from Value  .:. SBSP
	    					String depC18Value   				= ""; 					// from Value  .:. DEP/ BARRA DO GARCA, MT, 0255S04512W
	    					String destC18Value  				= ""; 					// from Value  .:.DEST/ BARRA DO GARCA, MT, 0255S04512W
	    					String dofGroup      				= ""; 					// DOF do plano de voo
	    					
	    					boolean isSearchCldMsg              = false;                // true: foi encontrado o ponto de fechamento da mensagem   false: nao foi encontrado ainda
	    						    					
	    					
	    					br = new BufferedReader(new FileReader(fileNameFPL ));
	    					while(br.ready()){
	    						   String linha = br.readLine().trim();
	    						   
	 
	    						   //Obtem matricula da aeronave
	    							Matcher matcherCldMsg = PATTERN_FPL_IS_END_MSG.matcher(linha);
	    							if( matcherCldMsg.find() ){ 
	    								isSearchCldMsg = true;
	    							}
	    							
	    							
	    						   
	    						    //Obtem matricula da aeronave
	    							Matcher matcherReg = PATTERN_FPL_IS_REG_AIRCRAFT.matcher(linha);
	    							if( matcherReg.find()  && (linha.indexOf("-") != -1) && ( linha.indexOf("(FPL-") != -1)  ){ 
	    								int tmpPos =  linha.indexOf("-", linha.indexOf("(FPL-") + 6 ) ;
	    								regAircraft = (linha.substring(linha.indexOf("(FPL-") + 5, (tmpPos == -1 ? linha.length() : tmpPos) ).trim());
	    								
	    								if( ! regAircraft.trim().matches("[A-Z]{3,10}")){
	    									regAircraft = "";
    									}
	    							}
	    						   
	
	    							
	    							
	    							//Obtem o tipo de mensagem  FPL, FPLs, CHG, DLA, CNL
	    							Matcher matcherIsFPL = PATTERN_FPL_IS_MSG_TYPE_FPL.matcher(linha);
	    							if( matcherIsFPL.find() ){ 
	    								countFPL++;
	    								msgType = ResumeMsg.MsgTypeE.FPL;
	    								
	    								Matcher matcherIsFPL_OPR_IFR = PATTERN_FPL_IS_MSG_TYPE_FPL_OP_IFR.matcher(linha);
		    							if( matcherIsFPL_OPR_IFR.find() ){ 
		    								operation = ResumeMsg.OpE.I;
		    								//System.out.println( "FPL_OPR_IFR: >>" + linha + "<<" );
		    							}
		    							
	    								Matcher matcherIsFPL_OPR_VFR = PATTERN_FPL_IS_MSG_TYPE_FPL_OP_VFR.matcher(linha);
		    							if( matcherIsFPL_OPR_VFR.find() ){ 
		    								operation = ResumeMsg.OpE.V;
		    								//System.out.println( "FPL_OPR_VFR: >>" + linha + "<<" );
		    							}
		    							
	    								Matcher matcherIsFPL_OPR_Y = PATTERN_FPL_IS_MSG_TYPE_FPL_OP_Y.matcher(linha);
		    							if( matcherIsFPL_OPR_Y.find() ){ 
		    								operation = ResumeMsg.OpE.Y;
		    								//System.out.println( "FPL_OPR_Y: >>" + linha + "<<" );
		    							}
		    							
	    								Matcher matcherIsFPL_OPR_Z = PATTERN_FPL_IS_MSG_TYPE_FPL_OP_Z.matcher(linha);
		    							if( matcherIsFPL_OPR_Z.find() ){ 
		    								operation = ResumeMsg.OpE.Z;
		    								//System.out.println( "FPL_OPR_Z: >>" + linha + "<<" );
		    							}
	    								
	    							}
	    							
	    							Matcher matcherIsFPLs = PATTERN_FPL_IS_MSG_TYPE_FPLS.matcher(linha);
	    							if( matcherIsFPLs.find() ){ 
	    								countFPLs++;
	    								msgType = ResumeMsg.MsgTypeE.FPLs;
	    								operation = ResumeMsg.OpE.V;
	    								//System.out.println( getLog(Log.INFO) + "linha countFPL: >>" + linha + "<<" );
	    							}
	    							
	    							Matcher matcherIsCHG = PATTERN_FPL_IS_MSG_TYPE_CHG.matcher(linha);
	    							if( matcherIsCHG.find() && (linha.indexOf("-") != -1) && (linha.indexOf("(CHG-") != -1) ){ 
	    								countCHG++;
	    								msgType = ResumeMsg.MsgTypeE.CHG;
	    								
	    								int tmpPos =  linha.indexOf("-", linha.indexOf("(CHG-") + 6 ) ;
	    								regAircraft = (linha.substring(linha.indexOf("(CHG-") + 5, (tmpPos == -1 ? linha.length() : tmpPos) ).trim());			
	    								//System.out.println( getLog(Log.INFO) + "linha countFPL: >>" + linha + "<<" );
	    							}
	    							
	    							Matcher matcherIsDLA = PATTERN_FPL_IS_MSG_TYPE_DLA.matcher(linha);
	    							if( matcherIsDLA.find()  && (linha.indexOf("-") != -1) && (linha.indexOf("(DLA-") != -1) ){ 
	    								countDLA++;
	    								msgType = ResumeMsg.MsgTypeE.DLA;
	    								int tmpPos =  linha.indexOf("-", linha.indexOf("(DLA-") + 6 ) ;
	    								regAircraft = (linha.substring(linha.indexOf("(DLA-") + 5, (tmpPos == -1 ? linha.length() : tmpPos) ).trim());
	    								//System.out.println( getLog(Log.INFO) + "linha countFPL: >>" + linha + "<<" );
	    							}
	    							
	    							Matcher matcherIsCNL = PATTERN_FPL_IS_MSG_TYPE_CNL.matcher(linha);
	    							if( matcherIsCNL.find() && (linha.indexOf("-") != -1) && (linha.indexOf("(CNL-") != -1) ){ 
	    								msgType = ResumeMsg.MsgTypeE.CNL;
	    								countCNL++;
	    								int tmpPos =  linha.indexOf("-", linha.indexOf("(CNL-") + 6 ) ;
	    								regAircraft = (linha.substring(linha.indexOf("(CNL-") + 5, (tmpPos == -1 ? linha.length() : tmpPos) ).trim());
	    								//System.out.println( getLog(Log.INFO) + "linha countFPL: >>" + linha + "<<" );
	    							}
	    							
	
	    							
	    							
	    							Matcher matcheraIsAircraftType = PATTERN_FPL_IS_AIRCFRT_TYPE.matcher(linha);
	    							if( matcheraIsAircraftType.find() ){ 
	    								
	    								if( linha.indexOf("/") != -1){
	    								  typeAircraft =  linha.substring( 1, linha.indexOf("/") );
	    								}else{
	    								  typeAircraft =  linha.trim();
	    								}
	    								
	    								if( typeAircraft.indexOf("-") != -1){	
	    									typeAircraft =  typeAircraft.substring( 0, typeAircraft.indexOf("-") );
	    								}    								
	    								
	    								if( typeAircraft.trim().matches("[0-9]{1,5}")){
	    									typeAircraft = "";
    									}
	    								
	    								if( ! typeAircraft.trim().matches("[A-Z0-9]{2,4}")){
	    									typeAircraft = "";
    									}

	    								 //  System.out.println( "typeAircraft: >>" + typeAircraft + "<<" );	  
	
	    							}
	    							
	    							
	    							
	    							
	    							
	    							Matcher matchCmteNameAnac = PATTERN_NAME_ANAC_CMTE.matcher(linha);
	    							if( matchCmteNameAnac.find() ){ 
	    								
	    								Matcher matchName = PATTERN_NAME.matcher(linha);
	    								if( matchName.find() ){ 
	    									cmteName =  linha.substring( matchName.start(), matchName.end() ).trim();
	    									linha = linha.substring(matchName.end());
	    								}
	    								
	    								Matcher matchANAC = PATTERN_ANAC.matcher(linha);
	    								if( matchANAC.find() ){ 
	    									cmteANAC =  linha.substring( matchANAC.start(), matchANAC.end() ).trim();
	    								}
	    								
	    								if( ! cmteANAC.trim().matches("[0-9]{6}")){
	    									cmteANAC = "";
    									}
   									    								
	    								 //System.out.println( "cmteName / cmteANAC: >>" + cmteName + " / " + cmteANAC + "<<" );	  
	
	    							}
	    							
	    							Matcher matchCopNameAnac = PATTERN_NAME_ANAC_PHONE_COP.matcher(linha);
	    							if( matchCopNameAnac.find() && (linha.indexOf("TELCTT") != -1) ){ 
	    								
	    								String linhaTmp = linha.replaceAll("PREENCHIDO POR", "");
	    								linhaTmp = linhaTmp.substring(0, linhaTmp.indexOf("TELCTT"));
	    								
	    								Matcher matchName = PATTERN_NAME.matcher(linhaTmp);
	    								if( matchName.find() ){ 
	    									copName =  linhaTmp.substring( matchName.start(), matchName.end() ).trim();
	    									
	    									if( copName.equals("OP") || copName.equals("OM") || copName.equals("A MESMA") || copName.equals("O MESMO") ||
	    										copName.equals("O MESMO OP") || copName.equals("O MESMPO") || copName.equals("O MJESMO") || copName.equals("O MSM") ||
	    										copName.equals("O MSMO") || copName.equals("O P") || copName.equals("O PR") || copName.equals("O PROPIO") ||
	    										copName.equals("O PROPRI") || copName.equals("O PROPRIO") ){
	    										
	    										copName = cmteName;
	    										
	    										
	    									}
	    									
	    									linhaTmp = linhaTmp.substring(matchName.end());
	    								}
	    								
	    								Matcher matchANAC = PATTERN_ANAC.matcher(linhaTmp);
	    								if( matchANAC.find() ){ 
	    									copANAC =  linhaTmp.substring( matchANAC.start(), matchANAC.end() ).trim();
	    									
	    									if( ! copANAC.trim().matches("[0-9]{6}")){
	    										copANAC = "";
	    									}
	    									
	    								}
							
	    								//System.out.println( "copName / copANAC  >>" + copName + " / " + copANAC + " / " + "<<" );	  
	
	    							}
	    							
	    							
	    							
	    							
	    							Matcher matchPhoneLine = PATTERN_PHONE_LINE.matcher(linha);
    								if( matchPhoneLine.find() && (linha.indexOf("TELCTT") != -1) ){ 
    									
    									String linhaTmp = linha.substring(linha.indexOf("TELCTT")).trim();
    									
    									Matcher matchPhone = PATTERN_PHONE.matcher(linhaTmp);
    									
	    									if( matchPhone.find() ){
	    										
	    										phone =  linhaTmp.substring( matchPhone.start(), matchPhone.end() ).trim();
	    										phone = phone.replaceAll("-", " ").trim();
	
	    										if( ! phone.trim().matches("[0-9 ]{7,30}")){
	    											phone = "";
		    									}
	    									 									
    									    }
	    									
	    									// System.out.println( "phone >>" + phone  + "<<" );	
    								}
	    							
	    							
    								

	    							Matcher matcherORGN = PATTERN_FPL_IS_ORIGN.matcher(linha);
	    							if( matcherORGN.find() ){ 
	    								
	    								Matcher matchORGN = PATTERN_ORGN.matcher(linha);
	    								if( matchORGN.find() ){ 
	    									orignValue =  linha.substring( matchORGN.start(), matchORGN.end() ).trim();
	    								}
	    								
	    								Matcher matchEOBT = PATTERN_EOBT.matcher(linha);
	    								if( matchEOBT.find() ){ 
	    									hourEOBT =  dfEOBT.format(Integer.parseInt(linha.substring( matchEOBT.start(), matchEOBT.end() ).trim()));
	    								}
	    								
	    								if( ! hourEOBT.trim().matches("[0-9]{4}")){
	    									hourEOBT = "";
    									}
	    								
	    								//System.out.println( "orignValue / hourEOBT: >>" + orignValue + " / " + hourEOBT +  "<<" );	
	    								
	    							}
	    							
	    							
	    							
	    							
	    							Matcher matcherOperator = PATTERN_OPERATOR.matcher(linha);
	    							
	    							if( matcherOperator.find() ){
	    								
	    								operatorOfAircfraft = linha.substring(matcherOperator.start() );
	    								operatorOfAircfraft = operatorOfAircfraft.replaceFirst(REGEX_OPERATOR, "");
	    								
	    								if( operatorOfAircfraft.indexOf("/") != -1 ){
	    									operatorOfAircfraft = operatorOfAircfraft.substring(0 , operatorOfAircfraft.indexOf("/" ) ); 
	    									operatorOfAircfraft = operatorOfAircfraft.replaceAll("RMK", "");
	    									operatorOfAircfraft = operatorOfAircfraft.replaceAll("RALT", "");
	    									operatorOfAircfraft = operatorOfAircfraft.replaceAll("FROM", "");
	    									operatorOfAircfraft = operatorOfAircfraft.replaceAll("EET", "");
	    									operatorOfAircfraft = operatorOfAircfraft.replaceAll("TGL", "");
	    									operatorOfAircfraft = operatorOfAircfraft.replaceAll("PER", "");
	    								}
	    								
    									operatorOfAircfraft = operatorOfAircfraft.replaceAll("\\)", "");
    									operatorOfAircfraft = operatorOfAircfraft.replaceAll("┤", "");
    									operatorOfAircfraft = operatorOfAircfraft.replaceAll("║", "-");
    									operatorOfAircfraft = operatorOfAircfraft.replaceAll("░", "");
    									operatorOfAircfraft = operatorOfAircfraft.replaceAll(",", "");
    									operatorOfAircfraft = operatorOfAircfraft.replaceAll(";", "");
    									operatorOfAircfraft = operatorOfAircfraft.replaceAll(":", "");
    									operatorOfAircfraft = operatorOfAircfraft.replaceAll("-", "");
    									
    									if(operatorOfAircfraft.trim().matches("[0-9]{1,30}")){
    										operatorOfAircfraft = "";
    									}
    									
    									operatorOfAircfraft = operatorOfAircfraft.trim();
	    								
	    								/*if(operatorOfAircfraft.length() > 5){
	    									  countPhone ++;
	    								} */
	    								
	    								// System.out.println( "operatorOfAircfraft: >>" + operatorOfAircfraft + "<<" );
	    							}
	    							
	    							
	    							
	    							
	    							Matcher matcherDH = PATTERN_FPL_DATE_HOUR.matcher(linha);
	    							if( matcherDH.find() ){ 
	    								dateGroupPresentation = linha.substring(linha.length() - 19).trim();
	    								dateGroupPresentation = (dateGroupPresentation.substring(0, 2) + dateGroupPresentation.substring(3, 5) + dateGroupPresentation.substring(6, 8) ).trim();
	    								dateGroupPresentationLOC = dateGroupPresentation;
	    								
	    								hourGroupPresentation = linha.substring(linha.length() - 8).trim();
	    								
	    								if( ! hourGroupPresentation.trim().matches("[0-9]{2}:[0-9]{2}:[0-9]{2}")){
	    									hourGroupPresentation = "";
    									}
	    							
	    								//System.out.println( "dateGroupPresentation: >>" + dateGroupPresentation + "<<" );
	    								//System.out.println( "hourGroupPresentation: >>" + hourGroupPresentation + "<<" );
	    							}
	    							
	    							
	    							
	    							Matcher matcherDOF = PATTERN_FPL_IS_DOF.matcher(linha);
	    							if( matcherDOF.find() && (linha.indexOf("DOF/") != -1) ){
	    								
	    								if(dofGroup != null && dofGroup.length() < 6){
	    									if( ! isSearchCldMsg ){ // caso nao encontrou o fechamento da mensaem .:. falha DOF externo
			    								dofGroup = ((linha.substring(linha.indexOf("DOF/") + 4, linha.indexOf("DOF/") + 10)).trim());			    								
			    								dofGroup = (dofGroup.substring(4) + dofGroup.substring(2, 4) + dofGroup.substring(0, 2) ); // Acertando DOF para DDMMAA
	    									}
	    								}
	    								
	    								if( ! dofGroup.trim().matches("[0-9]{6}")){
	    									dofGroup = "";
    									}
	    								
		    							//System.out.println( "dofGroup: >>" + dofGroup + "<<" );
	    							}
	    							
	    							
	    							
	    							
	    							Matcher matcherDest = PATTERN_FPL_IS_DEST.matcher(linha);
	    							if( matcherDest.find() ){ 
	    								destValue = linha.substring(1, 5).trim();
	    							}
	    							
	    							
	    							
	    							Matcher matcherFrom = PATTERN_FPL_IS_FROM_C18.matcher(linha);
	    							if( matcherFrom.find() && (linha.indexOf("FROM") != -1) ){ 
	    								String fromC18ValueTMP = linha.substring(linha.indexOf("FROM") + 5);
	    								int posProxRMK = fromC18ValueTMP.indexOf("/", 7);
	    								if(posProxRMK != -1 && posProxRMK >= 4){
	    									fromC18ValueTMP = fromC18ValueTMP.substring(0, posProxRMK - 4);
	    								}
	    								fromC18ValueTMP = fromC18ValueTMP.replace("/", "");
	    								fromC18ValueTMP = fromC18ValueTMP.trim();
	    								fromC18ValueTMP = fromC18ValueTMP.replaceAll(",", " -");
	    								fromC18ValueTMP = fromC18ValueTMP.replaceAll(";", " -");
	    								fromC18ValueTMP = fromC18ValueTMP.trim();
	    								
	    								if(fromC18ValueTMP != null && fromC18ValueTMP.length() > fromC18Value.length() ){ // Prevencao de edicao instavel para multiplos FROM/ no FPL
	    								  fromC18Value = fromC18ValueTMP;
	    								}	
	    								
	    							}
	    							
	    							
	    							
	    							Matcher matcherDepC18 = PATTERN_FPL_IS_DEP_C18.matcher(linha);
	    							if( matcherDepC18.find() && (linha.indexOf("DEP") != -1) ){ 
	    								String depC18ValueTMP = linha.substring(linha.indexOf("DEP") + 4);
	    								int posProxRMK = depC18ValueTMP.indexOf("/", 6);
	    								if(posProxRMK != -1 && posProxRMK >= 4){
	    									depC18ValueTMP = depC18ValueTMP.substring(0, posProxRMK - 4);
	    								}
	    								depC18ValueTMP = depC18ValueTMP.replace("/", "");
	    								depC18ValueTMP = depC18ValueTMP.trim();
	    								depC18ValueTMP = depC18ValueTMP.replaceAll(",", " -");
	    								depC18ValueTMP = depC18ValueTMP.replaceAll(";", " -");
	    								depC18ValueTMP = depC18ValueTMP.trim();
	    								
	    								if(depC18ValueTMP != null && depC18ValueTMP.length() > depC18Value.length() ){ // Prevencao de edicao instavel para multiplos DEP/ no FPL
	    									  depC18Value = depC18ValueTMP;
	    								}

	    							}
	    							
	    							
	    							
	    							Matcher matcherDestC18 = PATTERN_FPL_IS_DEST_C18.matcher(linha);
	    							if( matcherDestC18.find() && (linha.indexOf("DEST") != -1) ){ 
	    								String destC18ValueTMP = linha.substring(linha.indexOf("DEST") + 5);
	    								int posProxRMK = destC18ValueTMP.indexOf("/", 7);
	    								if(posProxRMK != -1 && posProxRMK >= 4){
	    									destC18ValueTMP = destC18ValueTMP.substring(0, posProxRMK - 4);
	    								}
	    								destC18ValueTMP = destC18ValueTMP.replace("/", "");
	    								destC18ValueTMP = destC18ValueTMP.trim();
	    								destC18ValueTMP = destC18ValueTMP.replaceAll(",", " -");
	    								destC18ValueTMP = destC18ValueTMP.replaceAll(";", " -");
	    								destC18ValueTMP = destC18ValueTMP.trim();
	    								
	    								if(destC18ValueTMP != null && destC18ValueTMP.length() > destC18Value.length() ){ // Prevencao de edicao instavel para multiplos FROM/ no FPL
	    									  destC18Value = destC18ValueTMP;
	    								}
	    							}	
	    						    
	    					}
	    					
	    					br.close();
	    					
	    					
	    					
	    					if(dofGroup != null && dofGroup.length() == 6){ // No caso de existencia de DOF o grupo dataGroup sera alterado
	    						
	    						if((dateGroupPresentationLOC != null && dateGroupPresentationLOC.length() == 6)){
		    						
	    							String tmpDap = (dateGroupPresentationLOC.substring(4) + dateGroupPresentationLOC.substring(2, 4) + dateGroupPresentationLOC.substring(0, 2) );
		    					    String tmpDof = (dofGroup.substring(4) + dofGroup.substring(2, 4) + dofGroup.substring(0, 2) );
		    						
		    					    if(tmpDap.compareTo(tmpDof) < 0){
		    						    dateGroupPresentationLOC = dofGroup;
		    					    }
		    					    
	    						}else{
	    							dateGroupPresentationLOC = dofGroup;
	    						}
	    						
	    					}
	    					
	    					
	    					
	    					keyFPL = regAircraft + "-" + dateGroupPresentationLOC + hourEOBT;
	
	    					
	    					
							Matcher matcherIsFROM = PATTERN_FPL_IS_FROM.matcher(fromC18Value.trim());
							if( matcherIsFROM.find() ){ 
								fromValue = fromC18Value.trim();
								
							}else{
								fromValue = "ZZZZ";
							}
							//System.out.println( "fromValue: >>" + fromValue + "<<"  + " : " + fromC18Value.trim());
	    					
							
							if(dateGroupPresentation != null && hourGroupPresentation != null){ // 131213  16:55:03  Buscar data hora no nome do arquivo caso nao seja possivel encontar no interior do arquivo
								if(dateGroupPresentation.length() != 6 ||  hourGroupPresentation.length() != 8){ 
									
									if( fileNameFPL.length() > 18){ // minimo comprimento de String - prevencao de falha
										String tmpfileNameFPL = fileNameFPL.replaceAll("_", "");
										dateGroupPresentation = tmpfileNameFPL.substring(tmpfileNameFPL.length() - 17, tmpfileNameFPL.length() - 11);
										hourGroupPresentation = tmpfileNameFPL.substring(tmpfileNameFPL.length() - 8, tmpfileNameFPL.length() - 6) + tmpfileNameFPL.substring(tmpfileNameFPL.length() - 6, tmpfileNameFPL.length() - 4);
										hourGroupPresentation = hourGroupPresentation.substring(0,2) + ":" + hourGroupPresentation.substring(2) + ":00";
										//System.out.println( "tmpfileNameFPL: >>" + tmpfileNameFPL + "<<");
										//System.out.println( "dateGroupPresentation: >>" + dateGroupPresentation + "<<");
										//System.out.println( "hourGroupPresentation: >>" + hourGroupPresentation + "<<");
									}
								}
							}
							
	    					// Objeto armazenado para confeccao de relatorio para estatistica
							rMsgForReport.setRegAircraft(regAircraft);
	    					rMsgForReport.setMsgType(msgType);
	    					rMsgForReport.setOperation(operation);
							rMsgForReport.setHourEOBT(hourEOBT);
							rMsgForReport.setFromValue(fromValue);
							rMsgForReport.setFilename(fileNameFPL);
							rMsgForReport.setOrignValue(orignValue);
							rMsgForReport.setDestValue(destValue);
							rMsgForReport.setTypeAircraft(typeAircraft);
							rMsgForReport.setCmteName(cmteName);
							rMsgForReport.setCmteANAC(cmteANAC);
							rMsgForReport.setCopName(copName);
							rMsgForReport.setCopANAC(copANAC);
							rMsgForReport.setPhone(phone);
							rMsgForReport.setOperatorOfAircfraft(operatorOfAircfraft);
							rMsgForReport.setDateGroupPresentation(dateGroupPresentation);
							rMsgForReport.setHourGroupPresentation(hourGroupPresentation);
							rMsgForReport.setKeyFPL(keyFPL);
							rMsgForReport.setFromC18Value(fromC18Value);
							rMsgForReport.setDepC18Value(depC18Value);
							rMsgForReport.setDestC18Value(destC18Value);
							rMsgForReport.setDofGroup(dofGroup);
							
							//if(dofGroup.startsWith("13")){ // Verificar falhas
								//System.out.println( rMsgForReport.getLineRegister(",") + "<<");
							//}
							
							if ( IS_GENERATE_REPORTY_FOR_STATISTICS  == 1 ){ // popula memoria para gerar relatorio estatistico
									
    							Matcher matcherDate = PATTERN_IS_DATE.matcher(rMsgForReport.getDateGroupPresentation());
    							Matcher matcherHour = PATTERN_IS_HOUR.matcher(rMsgForReport.getHourGroupPresentation());
    							Matcher matcherRegAircraft = PATTERN_FPL_VALIDADE_REG_AIRCRAFT.matcher(rMsgForReport.getRegAircraft());
    							
    							//_FPL_VALIDADE_REG_AIRCRAFT
    							
    							if( matcherDate.find() && matcherHour.find() && matcherRegAircraft.find()){ 
    								tSetOfResumeMsg.add(rMsgForReport); // Adiciona objeto
    							}
	
							}

							
							
							
	    					Matcher matcherDest = PATTERN_FPL_KEY_INDEX.matcher(keyFPL); // Primeiro precisa haver um keyFPL valido
	    					if( matcherDest.find() ){ 
	    						
	    						if( ( destValue.equals("ZZZZ") || orignValue.equals("ZZZZ") || fromC18Value.length() > 4 )){ // caso haja motivo para gerar loc
	    							
	    							
	    							// Objeto armazenado para embasar criacao de mensagems LOC
	    							ResumeMsg rMsgForLOC = new ResumeMsg();
	    							rMsgForLOC.setHourEOBT(hourEOBT);
	    							rMsgForLOC.setFilename(fileNameFPL);
	    							rMsgForLOC.setOrignValue(orignValue);
	    							rMsgForLOC.setDestValue(destValue);
	    							rMsgForLOC.setDateGroupPresentation(dateGroupPresentationLOC);
	    							rMsgForLOC.setKeyFPL(keyFPL);
	    							rMsgForLOC.setFromC18Value(fromC18Value);
	    							rMsgForLOC.setDepC18Value(depC18Value);
	    							rMsgForLOC.setDestC18Value(destC18Value);
	    							
	    							mapFPLForLoc.put(keyFPL, rMsgForLOC);
	    							
	    						}
	    						
	    					}

	    					
	    				} catch (FileNotFoundException e) {
	    					System.out.println( getLog(Log.ERROR) + e.getMessage());
	    				} catch (IOException e) {
	    					System.out.println( getLog(Log.ERROR) + e.getMessage());
	    				}

    			}
    			
    			System.out.println(" ");
    			System.out.println(  "Lidas: ");
    			System.out.println(  "Mensagens   FPL   		: " +  String.format("%8s", dfCountNumber.format(countFPL) ));
    			System.out.println(  "Mensagens   NTV	  		: " +  String.format("%8s", dfCountNumber.format(countFPLs) ));
    			System.out.println(  "Mensagens   CHG   		: " +  String.format("%8s", dfCountNumber.format(countCHG) ));
    			System.out.println(  "Mensagens   DLA   		: " +  String.format("%8s", dfCountNumber.format(countDLA) ));
    			System.out.println(  "Mensagens   CNL   		: " +  String.format("%8s", dfCountNumber.format(countCNL) ));
    			int readTotal = ( countFPL + countFPLs + countCHG + countDLA + countCNL );
    			System.out.println(  "Mensagens   TOTAL 		: " +  String.format("%8s", dfCountNumber.format( readTotal ) ));
    			System.out.println(" ");
    			System.out.println(  "Report: ");
    			System.out.println(  "Msgs NAO Identificadas			: " +  String.format("%15s", dfCountNumber.format( listOfFPLFiles.size() - readTotal ) ) + " ( " + dfPercent.format(  (( ( double )listOfFPLFiles.size() - readTotal ) / ( (double) listOfFPLFiles.size() )) * 100.00   ) + " % )");
    			
    			//System.out.println(  "countphone			: "  + countPhone );
    			
    			if ( IS_GENERATE_REPORTY_FOR_STATISTICS  == 1 ){ // popula memoria para gerar relatorio estatistico
    			System.out.println(  "Msgs EXCLUIDAS do DB Estatístico	: " +  String.format("%15s", dfCountNumber.format( listOfFPLFiles.size() - tSetOfResumeMsg.size() ) ) + " ( " + dfPercent.format(  (( ( double )listOfFPLFiles.size() - tSetOfResumeMsg.size() ) / ( (double) listOfFPLFiles.size() )) * 100.00   ) + " % )");
    			}
    			
    			System.out.println(" ");
    			
    			System.out.println( getLog(Log.INFO) + "Leitura de arquivo (s) realizada com sucesso." );
    			System.out.println( getLog(Log.INFO) + "Criando um indice de " + mapFPLForLoc.size() + " entrada (s)." );
    			System.out.println( getLog(Log.INFO) + "Criada entrada (s) de consulta para processamento de LOC." );
    				
    			
    			
// FASE 2  -  GERAR MENSAGENS DE FECHAMENTO DE SVC
    			
    			
    			// Armazenadores de numero de mensagens
    			//int numberOfISEMT = 0;
    			//int numberOfLOC = 0;
    			//int numberOfMOV = 0;
    			//int numberOfMTE = 0;
    			int numberFirstMsgCONFAC = 0; // Numero de ordem da primeira mensagem CONFAC
    			int numberFirstISE = 0; //Numero de ordem da primeira mensagem ISE
    			String outFileEnd = ""; // Texto final que sera enviado para o arquivo
    			
    			
    			
    			ArrayList<String>  listOfConfacFiles = new ArrayList<String>(); //armazena no nome dos arquivos de  CONFAC existentes pasta de CONFAC
    			TreeSet<Mov>  tSetOfMsgConfacInFiles = new TreeSet<Mov>(); //armazena cada uma das mensagens CONFAC contido no arquivo de CONFAC
    			TreeSet<Ise>  tSetOfMsgIseInFiles = new TreeSet<Ise>(); //armazena cada uma das mensagens ISE contidas no arquivo de CONFAC
    			ArrayList<String>  listOfMsgLocInFiles = new ArrayList<String>(); //armazena cada uma das mensagens LOC obtidas no processamento
    			
    			File diretorioCONFAC = new File(ADDRESS_CONFAC_OF_TARGET_FOLDER);  
    			String [] arquivosCONFAC = diretorioCONFAC.list(filtro);  
    			
    			
    			if(arquivosCONFAC == null){
    				
    				System.out.println( getLog(Log.FATAL) + "Endereco para pasta de arquivos CONFAC NAO encontrado, ou a pasta esta vazia. Acertar parametros do arquivo config.properties" );
	    			System.exit(0);
	    			
    			}else{
    				for(int i = 0; i < arquivosCONFAC.length; i++) { // Obtem o nome de todos os arquivos da pasta ...\\CONFAC
	    				listOfConfacFiles.add(arquivosCONFAC[i]);
	    			} 
    			}
    			
    			System.out.println( getLog(Log.INFO) + "Iniciando a leitura de " + listOfConfacFiles.size() + " arquivo (s) CONFAC.");
    			System.out.println( getLog(Log.INFO) + "Lendo pasta " + ADDRESS_CONFAC_OF_TARGET_FOLDER  );
    			System.out.println( getLog(Log.INFO) + "Efetuando cruzamento de informacoes (FPL e FPL(s) X CONFAC)..." );
    	    			
    			System.out.println( getLog(Log.INFO) + "Iniciando de geracao de " + listOfConfacFiles.size() + " relatorio (s)..." );
    			System.out.println( getLog(Log.INFO) + "Enviando relatorio (s) para pasta " + ADDRESS_GENERATE_FILES_REPORT + "\\" + "..." );
    			
    			// PROCESSA CADA ARQUIVO INDIVIDUALMENTE
    			int countFileGenerate = 0; // conta a quantidade de arquivos criados.
    			
    			for (Iterator<String> iter = listOfConfacFiles.iterator(); iter.hasNext();) {	 // Efetua a leitura de cada um dos arquivos CONFAC para efetuar o processamento
    				
        			String reportMalformedMovInConfac = ""; // Informa os reports de MSG CONFAC malformadas encontradas no arquivo de CONFAC de leitura .:. Serao ignoradas na composicao do relatorio
        			String reportRepeatMovInConfac = ""; // Informa os reports de MSG CONFAC repetidas encontradas no arquivo de CONFAC de leitura .:. Serao ignoradas na composicao do relatorio

    				String fileNameCONFAC = iter.next(); // Nome do arquivo CONFAC que sera realizada a leitura
	    				
	    				tSetOfMsgConfacInFiles.clear(); // Limpa Array
	    				tSetOfMsgIseInFiles.clear(); // Limpa Array
	    				listOfMsgLocInFiles.clear(); // Limpa Array
	    				countFileGenerate ++; // Este conta
	    				
	    				BufferedReader br;
	    				try {
	    				
	    					
	    					// Efetua a leitura de todas as mensagens MOV do relatorio confac Validando as entradas
	    					br = new BufferedReader(new FileReader(ADDRESS_CONFAC_OF_TARGET_FOLDER + "\\" + fileNameCONFAC ));
	    					fileNameCONFAC = fileNameCONFAC.toLowerCase(); // Apos leitura de arquivo
	    					int countCONFAC = 0;
	    					
	    					while(br.ready()){ // Obtendo a listagem de Mensagem MOV da CONFAC
	    						
	    						   String linha = br.readLine().trim();
	    						   
	    							Matcher matcherCONFAC = PATTERN_IS_VALID_CONFAC.matcher(linha);
	    							Matcher matcherISE = PATTERN_IS_VALID_ISE.matcher(linha);
	    							Matcher matcherCONFACORMalformed = PATTERN_IS_VALID_CONFAC_OR_MALFORMED.matcher(linha);
	    							
	    							int monitorRepeat = tSetOfMsgConfacInFiles.size(); // Monitora se há mensagens repetidas no relatorio CONFAC original
	    							
	    							if( matcherCONFAC.find() && (linha.indexOf("(MOV-") != -1) ){ // Verifica se a linha lida eh uma linha de mensagem confac
	    								
	    								// Obtendo numeracao da primeira MOV da CONFAC
	    								if(tSetOfMsgConfacInFiles.size() == 0){ // Primeira mensagem CONFAC encontrada
	    									String numberCONFACForCorrection = linha.substring(0, linha.indexOf("(MOV-")).trim();
	    									numberFirstMsgCONFAC = Integer.parseInt(numberCONFACForCorrection);
	    								}
	    								
	    								Mov mov = new Mov();
	    								mov.setMov(linha.substring(linha.indexOf("(MOV-")));
	    								tSetOfMsgConfacInFiles.add(mov); // Adiciona MSG CONFAC com numeracao ajustada
	    								
	    								if ( monitorRepeat == tSetOfMsgConfacInFiles.size()){
	    									reportRepeatMovInConfac += linha + LINE_SEPARATOR;
	    								}
	    								
	    							}else if( matcherCONFACORMalformed.find() && (linha.indexOf("(MOV-") != -1) ){
	    								reportMalformedMovInConfac += linha + LINE_SEPARATOR;
	    							} 
	    							
	    							//verifica se a linha eh uma mensagem ISE
	    							if( matcherISE.find() && (linha.indexOf("(ISE-") != -1)) {
	    								
	    								// Obter a numeracao da primeira ISE
	    								if (tSetOfMsgIseInFiles.size() == 0){
	    									String numeroPrimeiraISE = linha.substring(0, linha.indexOf("(ISE-")).trim();
	    									numberFirstISE = Integer.parseInt(numeroPrimeiraISE);
	    								}
	    								
	    								Ise ise = new Ise();
	    								ise.setIse(linha.substring(linha.indexOf("(ISE-")));
	    								tSetOfMsgIseInFiles.add(ise); //adiciona a mensagem ISE
	    							}
	    					}
	    					
	    					
	    					
	    					
	    					for (Iterator<Mov> iterH = tSetOfMsgConfacInFiles.iterator(); iterH.hasNext();) {
	    						
	    						Mov registroMOV = iterH.next();
								
								int numberTmp = numberFirstMsgCONFAC + countCONFAC;
								registroMOV.setMov(registroMOV.getMov().replace(("(MOV-"), (String.valueOf(numberTmp) + "(MOV-")));  // (MOV-  Garante o replace somente da numeracao
								
								String movText = registroMOV.getMov();
								
								//tSetOfMsgConfacInFiles.add(linha); // Adiciona MSG CONFAC con numeracao ajustada
	

								countCONFAC ++; // incrementa valor de CONFAC
								
								Matcher matcherZZZZ = PATTERN_CONFAC_ZZZZ_GROUP.matcher(movText);
								
								if( matcherZZZZ.find() && (movText.indexOf("(MOV-") != -1) ){ // Verifica se a movText de mensagem CONFAC encontrada possui ou nao FROM, ORIGEM OU DESTINO com grupo ZZZZ
									
									
									
									// GABARITO
									// 43036(MOV-PRSCX-V-SBSP-SBMT-ZZZZ-119496-1612131000)
									// SVC S/N RETEL /MOV, ANV PRPVC, LOC CAMPO 10 [ FROM ], LOC CAMPO 11 [ ORIGEM ], LOC CAMPO 12 [ DESTINO ] 

									String numberCONFAC = movText.substring(0, movText.indexOf("(MOV-")).trim();
									String regiAircraftInCONFAC = movText.substring((movText.indexOf("(MOV-") + 5), movText.length() - 36 ).trim(); // Matricula da aeronave na CONFAC .:. PPYSE							
									String keyCONFAC = regiAircraftInCONFAC + "-" + movText.substring(movText.length() - 11, movText.length() - 1).trim() ;  // Esta variave representa uma forma de identicar uma confac independente do seu numero ( PTYSZ-2712131950 )
									    									
									String gabaritoLoc = "SVC S/N RETEL [NUMBER]/MOV, ANV [REGISTRATION][LOC_CAMPO_10_FROM][LOC_CAMPO_11_ORIGIN][LOC_CAMPO_12_DESTINATION]";
									
									String temp = gabaritoLoc.replace("[NUMBER]", numberCONFAC);
									temp = temp.replace("[REGISTRATION]", regiAircraftInCONFAC);

									ResumeMsg fplInfo = mapFPLForLoc.get(keyCONFAC);

									
									if(fplInfo != null){
										
										if(fplInfo.getFromC18Value() != null && fplInfo.getFromC18Value().length() > 4){ // from acima de 4 letras SBSP
											temp = temp.replace("[LOC_CAMPO_10_FROM]", (", LOC CAMPO 10 " + fplInfo.getFromC18Value()));
										}else{
											temp = temp.replace("[LOC_CAMPO_10_FROM]", "");
										}
										
										if(fplInfo.getDepC18Value() != null && fplInfo.getDepC18Value().length() > 4){ // dep acima de 4 letras SBSP .:. prevencao de falha
											temp = temp.replace("[LOC_CAMPO_11_ORIGIN]", (", LOC CAMPO 11 " + fplInfo.getDepC18Value()));
										}else{
											temp = temp.replace("[LOC_CAMPO_11_ORIGIN]", "");
										}
										
										if(fplInfo.getDestC18Value() != null && fplInfo.getDestC18Value().length() > 4){ // dest acima de 4 letras SBSP .:. prevencao de falha
											temp = temp.replace("[LOC_CAMPO_12_DESTINATION]", (", LOC CAMPO 12 " + fplInfo.getDestC18Value()));
										}else{
											temp = temp.replace("[LOC_CAMPO_12_DESTINATION]", "");
										}
										

									}else{
										temp = temp.replace("[LOC_CAMPO_10_FROM]", "");
										temp = temp.replace("[LOC_CAMPO_11_ORIGIN]", "");
										temp = temp.replace("[LOC_CAMPO_12_DESTINATION]", "");
									}
									
									listOfMsgLocInFiles.add(temp);
									
									
								}
							}
	    					
	    					
	    					//for (Iterator<Mov> iterH = tSetOfMsgConfacInFiles.iterator(); iterH.hasNext();) { // Para teste de mensagens
 							//  System.out.println(">>" +  iterH.next() + "<<");
 						    //}
	    					
	    					
	    					
	    					//Obtendo a data pelo nome do arquivo CONFAC
	    					//TODO Otimizar metodo para obtencao da data
	    					Matcher matcherFileConfac = PATTERN_FPL_IS_NAME_FILE_CONFAC.matcher(fileNameCONFAC.trim());
	    					String dateOfCONFAC = "";
	    					if( matcherFileConfac.find() && (fileNameCONFAC.indexOf(".txt") != -1) ){ 
	    						dateOfCONFAC = fileNameCONFAC.trim().substring(fileNameCONFAC.trim().length() - 10, fileNameCONFAC.trim().indexOf(".txt")).trim();
	    						dateOfCONFAC = dateOfCONFAC.substring(0, 2) + "/" + dateOfCONFAC.substring(2, 4) + "/"  + dateOfCONFAC.substring(4);  // Inserir as barras na data  .:. 010114 to 01/01/14
	    					}else{
	    						dateOfCONFAC = "DD/MM/AA";
	    					}
	    					
	    					
	    					int numberMovFirst = numberFirstMsgCONFAC; // Numero da primeira mensagem CONFAC 
	    					int numberMovLast = tSetOfMsgConfacInFiles.size() + ( numberFirstMsgCONFAC - 1 ); // Numero da ultima mensagem Confac
	    					
	    					
	    					//Cria arquivo de mensagem
	    					outFileEnd = "";
	    					outFileEnd += HEADER_OF_FILE + " " + dateOfCONFAC + LINE_SEPARATOR + LINE_SEPARATOR + LINE_SEPARATOR;
	    					
	    					outFileEnd += TITLE_NUMBER_OF_ISE + " " + dfCountC.format(tSetOfMsgIseInFiles.size()) + LINE_SEPARATOR + LINE_SEPARATOR;
	    					outFileEnd += TITLE_NUMBER_OF_LOC + " " + dfCountC.format(listOfMsgLocInFiles.size()) + LINE_SEPARATOR + LINE_SEPARATOR;
	    					outFileEnd += TITLE_NUMBER_OF_MOV + " " + dfCountM.format(tSetOfMsgConfacInFiles.size()) + LINE_SEPARATOR + LINE_SEPARATOR;
	    					outFileEnd += TITLE_NUMBER_OF_MTE + LINE_SEPARATOR + LINE_SEPARATOR + LINE_SEPARATOR;
	    					
	    					outFileEnd += 	"=====================================================" + LINE_SEPARATOR + 
	    									"OPERADOR AIS, VERIFIQUE SE A NUMERACAO DA PRIMEIRA " + LINE_SEPARATOR +
	    									"MOV (" + numberFirstMsgCONFAC + ") ESTA CORRETA." + " CASO CONTRARIO, CORRIJA O " + LINE_SEPARATOR +
	    									"NUMERO DA PRIMEIRA MOV NO ARQUIVO: <" + fileNameCONFAC + ">" + LINE_SEPARATOR +
	    									"EM SEGUIDA, GERE O RELATORIO NOVAMENTE.";
	
	    					
	    					
	    					
	    					// QUEBRANDO CONFAC EM BLOCOS PARA SAIDA
	    					if(tSetOfMsgConfacInFiles.size() > 0){
	    					   outFileEnd += 	LINE_SEPARATOR + "=====================================================" 
	    							   			+ LINE_SEPARATOR + LINE_SEPARATOR  + TITLE_MOV + " ( DE " + numberMovFirst + " ATÉ " + numberMovLast + " )"+ LINE_SEPARATOR ;
	    					}
	    					
	    					
	    					double numberOfBlocksMOVAvaliation = ((double) tSetOfMsgConfacInFiles.size() / ( double ) NUMBER_OF_MSG_FOR_BLOCK_MOV ); // trunca o valor
	    					int numberOfBlocksMOV = 0;
	    					if( numberOfBlocksMOVAvaliation > ((int) numberOfBlocksMOVAvaliation) ){
	    						numberOfBlocksMOV = ((int) numberOfBlocksMOVAvaliation);
	    						numberOfBlocksMOV ++;
	    					}else{
	    						numberOfBlocksMOV = ((int) numberOfBlocksMOVAvaliation);
	    					}
	    									
	    					int countBlockMOV = 0; // contador para bloco de mensagens
	    					int countLeafMOV = 0;
	    					for ( Mov msgMov : tSetOfMsgConfacInFiles) { 
	    						
	    						String msgMovText = msgMov.getMov();
	    						countBlockMOV ++;
	
	    						if(countBlockMOV == 1 || countBlockMOV > NUMBER_OF_MSG_FOR_BLOCK_MOV){
	    							countBlockMOV = 1;
	    							countLeafMOV++;
	    							outFileEnd += LINE_SEPARATOR + LINE_SEPARATOR + LINE_SEPARATOR + TITLE_HEADER_OF_BLOCKS_MOV  + " " + dateOfCONFAC + " FOLHA " + countLeafMOV + "/" + numberOfBlocksMOV + LINE_SEPARATOR + LINE_SEPARATOR;
	    						}
	    						
	    						//outFileEnd += dfCountM.format(countMsgMOV) + ": " + msgMovText + LINE_SEPARATOR; // Destinado para teste
	    						outFileEnd += msgMovText + LINE_SEPARATOR;
	    					}
	    				
	    					
	    					
	    					
	    					// QUEBRANDO LOC EM BLOCOS PARA SAIDA
	    					if(listOfMsgLocInFiles.size() > 0){
	    						outFileEnd += 	LINE_SEPARATOR + "=====================================================" 
	    										+ LINE_SEPARATOR  + LINE_SEPARATOR + LINE_SEPARATOR + TITLE_LOC + LINE_SEPARATOR ;
	    					}
	
	    					double numberOfBlocksLOCAvaliation = ((double) listOfMsgLocInFiles.size() / ( double ) NUMBER_OF_MSG_FOR_BLOCK_LOC ); // trunca o valor
	    					int numberOfBlocksLOC = 0;
	    					if( numberOfBlocksLOCAvaliation > ((int) numberOfBlocksLOCAvaliation) ){
	    						numberOfBlocksLOC = ((int) numberOfBlocksLOCAvaliation);
	    						numberOfBlocksLOC ++;
	    					}else{
	    						numberOfBlocksLOC = ((int) numberOfBlocksLOCAvaliation);
	    					}
	    									
	    					int countBlockLOC = 0; // contador para bloco de mensagens
	    					int countLeafLOC = 0;
	    					for (String msgLOC  : listOfMsgLocInFiles) { 
	    						countBlockLOC ++;
	
	    						if(countBlockLOC == 1 || countBlockLOC > NUMBER_OF_MSG_FOR_BLOCK_LOC){
	    							countBlockLOC = 1;
	    							countLeafLOC++;
	    							outFileEnd += LINE_SEPARATOR + LINE_SEPARATOR + LINE_SEPARATOR + TITLE_HEADER_OF_BLOCKS_LOC  + " " + dateOfCONFAC + " FOLHA " + countLeafLOC + "/" + numberOfBlocksLOC + LINE_SEPARATOR + LINE_SEPARATOR;
	    						}
	    						
	    						//outFileEnd += dfCountM.format(countMsgLOC) + ": " + msgLOC + LINE_SEPARATOR; // Destinado para teste
	    						outFileEnd += msgLOC + LINE_SEPARATOR;
	    					}
	    					
	    				
	    					
	    					// QUEBRANDO ISE EM BLOCOS PARA SAIDA
	    					if(tSetOfMsgIseInFiles.size() > 0){
	    					   outFileEnd += 	LINE_SEPARATOR + "=====================================================" 
	    							   			+ LINE_SEPARATOR + LINE_SEPARATOR  + TITLE_ISE + " ( " + tSetOfMsgIseInFiles.size() + " MENSAGEM(S) )"+ LINE_SEPARATOR ;
	    					}
	    					
	    					
	    					double numberOfBlocksISEAvaliation = ((double) tSetOfMsgIseInFiles.size() / ( double ) NUMBER_OF_MSG_FOR_BLOCK_ISE ); // trunca o valor
	    					int numberOfBlocksISE = 0;
	    					if( numberOfBlocksISEAvaliation > ((int) numberOfBlocksISEAvaliation) ){
	    						numberOfBlocksISE = ((int) numberOfBlocksISEAvaliation);
	    						numberOfBlocksISE ++;
	    					}else{
	    						numberOfBlocksISE = ((int) numberOfBlocksISEAvaliation);
	    					}
	    									
	    					int countBlockISE = 0; // contador para bloco de mensagens
	    					int countLeafISE = 0;
	    					int numberISE = numberFirstISE;
	    					
	    					for ( Ise msgIse : tSetOfMsgIseInFiles) { 
	    						
	    						String numeroISE = "";
	    						if (IS_NUMERATION_FOR_ISE == 1) { //incluir a numeracao nas ISE
	    							numeroISE = numberISE + "";
	    							numberISE++;
	    						}
	    						
	    						String msgIseText = numeroISE + msgIse.getIse();
	    						countBlockISE ++;
	
	    						if(countBlockISE == 1 || countBlockISE > NUMBER_OF_MSG_FOR_BLOCK_ISE){
	    							countBlockISE = 1;
	    							countLeafISE++;
	    							outFileEnd += LINE_SEPARATOR + LINE_SEPARATOR + LINE_SEPARATOR + TITLE_HEADER_OF_BLOCKS_ISE  + " " + dateOfCONFAC + " FOLHA " + countLeafISE + "/" + numberOfBlocksISE + LINE_SEPARATOR + LINE_SEPARATOR;
	    						}
	    						
	    						//outFileEnd += dfCountM.format(countMsgMOV) + ": " + msgMovText + LINE_SEPARATOR; // Destinado para teste
	    						outFileEnd += msgIseText + LINE_SEPARATOR;
	    					}
	    					
	    					
	    					
	    					// Armazenara um report sobre as mensagens MOV repetidas e mal formadas
	    					String reportMovFailRead = ""; 
	    				
	    					if( (! reportMalformedMovInConfac.equals("")) || (! reportRepeatMovInConfac.equals("")) ){
	    						
	    						reportMovFailRead += LINE_SEPARATOR + LINE_SEPARATOR + LINE_SEPARATOR + LINE_SEPARATOR + "----------------------------------------------------------------------------------" + LINE_SEPARATOR + LINE_SEPARATOR;
	    						
	    						reportMovFailRead += LINE_SEPARATOR + "REPORT DE INADEQUAÇÕES DE MENSAGENS MOV PERTENCENTES AO ARQUIVO " + fileNameCONFAC.toUpperCase() + LINE_SEPARATOR + LINE_SEPARATOR ;
	    						
	    						if( ! reportMalformedMovInConfac.equals("")){
		    						reportMovFailRead += LINE_SEPARATOR + "  MENSAGENS MOV MALFORMADAS: " + LINE_SEPARATOR + LINE_SEPARATOR;
		    						reportMovFailRead += reportMalformedMovInConfac;
		    					}
		    					
		    					if( ! reportRepeatMovInConfac.equals("")){
		    						reportMovFailRead += LINE_SEPARATOR + "  MENSAGENS MOV REPETIDAS: " + LINE_SEPARATOR + LINE_SEPARATOR ;
		    						reportMovFailRead += reportRepeatMovInConfac;
		    					}
		    					
		    					reportMovFailRead += LINE_SEPARATOR + LINE_SEPARATOR + "OBS.: AS MENSAGENS MOV INADEQUADAS FORAM IGNORADAS NA COMPOSIÇÃO DESTE RELATÓRIO";
		    					
		    					reportMovFailRead += LINE_SEPARATOR + LINE_SEPARATOR + "----------------------------------------------------------------------------------" + LINE_SEPARATOR + LINE_SEPARATOR;
		    					
		    					outFileEnd += reportMovFailRead;
		    					
	    					}
	    					
	    					outFileEnd += LINE_SEPARATOR + "========== F I M   D O   R E L A T O R I O ==========";
	    												    
	 
	    					
	    					String nameFileGenerate = TITLE_FILE_NAME_GENERATE + fileNameCONFAC.toUpperCase();
	    					
	    					FileOutputStream out = new FileOutputStream(ADDRESS_GENERATE_FILES_REPORT + "\\" + nameFileGenerate);
	    					out.write(outFileEnd.getBytes());
	    					out.close();
	    					
	    					br.close();
	    					
	    					System.out.println( getLog(Log.INFO) +  dfCountC.format(countFileGenerate) + ": " + nameFileGenerate );
	    					
	    				} catch (FileNotFoundException e) {
	    			    	System.out.println( getLog(Log.ERROR) + e.getMessage());
	    				} catch (IOException e) {
	    					System.out.println( getLog(Log.ERROR) + e.getMessage());
	    				}
	
    			
				}
    			
				if ( IS_GENERATE_REPORTY_FOR_STATISTICS  == 1 ){ // Efetua o processamento do relatorio estatistico
					
					try{
						
						System.out.println( getLog(Log.INFO) + "Iniciando de geracao de relatorio para fins estatisticos..." );
						
						String header = "";
						
						String outReportFile = "";
						
						String firstMsg = "ZZZZZZZZZZZZ";
						String firstMsgDateTime = "";
						
						String lastMsg = "";
						String lastMsgDateTime = "";
						
						
						int countMsg = 0;
						for ( ResumeMsg resumeMsg : tSetOfResumeMsg) { 
							
							if(firstMsg.compareTo(resumeMsg.getGroupDataHoraText()) > 0){
								firstMsg = resumeMsg.getGroupDataHoraText();
								firstMsgDateTime = resumeMsg.getGroupDataHoraBRS();
							}
							
							if(lastMsg.compareTo(resumeMsg.getGroupDataHoraText()) < 0){
								lastMsg = resumeMsg.getGroupDataHoraText();
								lastMsgDateTime = resumeMsg.getGroupDataHoraBRS();
							}
								
						
							
							countMsg++;
					
							// Selecioa qual o tipo de relatorio gerado
							String resumeMsgText = "";
							if(TYPE_FOR_REPORT_STATICS.equals(ResumeMsg.ReportTypeE.FULL.getValue())){
								resumeMsgText = dfCountG.format(countMsg) + SEPARATOR_FOR_REPORT_STATICS + resumeMsg.getLineRegisterFull(SEPARATOR_FOR_REPORT_STATICS);
							}else if(TYPE_FOR_REPORT_STATICS.equals(ResumeMsg.ReportTypeE.MODERATE.getValue())){
								resumeMsgText = dfCountG.format(countMsg) + SEPARATOR_FOR_REPORT_STATICS + resumeMsg.getLineRegisterModerate(SEPARATOR_FOR_REPORT_STATICS);
							}else{
								resumeMsgText = dfCountG.format(countMsg) + SEPARATOR_FOR_REPORT_STATICS + resumeMsg.getLineRegisterSimple(SEPARATOR_FOR_REPORT_STATICS);
							}
							
							outReportFile +=  resumeMsgText + LINE_SEPARATOR;
						}
						
						
						
						String tmpTitle = ""; // linha de colunas orientadoreas
						String tmpTitleLine = ""; // Linha de titulos de colunas para arquivo exportado
						
						// Selecioa qual o cabecario tipo de relatorio gerado
						if(TYPE_FOR_REPORT_STATICS.equals(ResumeMsg.ReportTypeE.FULL.getValue())){
							tmpTitle = ResumeMsg.ReportTitleE.FULL.getValue();
							tmpTitleLine = ResumeMsg.getHeaderLineFull(SEPARATOR_FOR_REPORT_STATICS);
						}else if(TYPE_FOR_REPORT_STATICS.equals(ResumeMsg.ReportTypeE.MODERATE.getValue())){
							tmpTitle = ResumeMsg.ReportTitleE.MODERATE.getValue();
							tmpTitleLine = ResumeMsg.getHeaderLineModerate(SEPARATOR_FOR_REPORT_STATICS);
						}else{
							tmpTitle = ResumeMsg.ReportTitleE.SIMPLE.getValue();
							tmpTitleLine = ResumeMsg.getHeaderLineSimple(SEPARATOR_FOR_REPORT_STATICS);
						}
						
						
						// Cabecario do relatorio
						header += "RELATÓRIO DE REGISTROS DE MENSAGENS ATS" + LINE_SEPARATOR + LINE_SEPARATOR + LINE_SEPARATOR;
						header += "Intervalo dos registros _____________  DE " + firstMsgDateTime + " ATÉ " + lastMsgDateTime + LINE_SEPARATOR + LINE_SEPARATOR;
						header += "Número de registros     _____________  " + tSetOfResumeMsg.size() + LINE_SEPARATOR + LINE_SEPARATOR;
						
						
						header += "Cabeçario:              _____________  " + tmpTitle  + LINE_SEPARATOR + LINE_SEPARATOR + LINE_SEPARATOR ;
						
						
						header += tmpTitleLine + LINE_SEPARATOR ; // Linha usada para nome das colunas
						
						
						System.out.println( getLog(Log.INFO) + "Enviando relatorio para pasta " + ADDRESS_GENERATE_FILES_REPORT + "\\" + "..." );
						
						String tmpAddressFileStatistics = ADDRESS_GENERATE_FILES_REPORT + "\\" + TITLE_REPORT_FOR_STATISTICS + ftDateFile.format( new Date() )  + ".TXT";
						
						FileOutputStream out = new FileOutputStream(tmpAddressFileStatistics);
						out.write( (header + outReportFile).getBytes());
						out.close();
						
						System.out.println( getLog(Log.INFO) + "Gerado Arquivo: " + tmpAddressFileStatistics);
						
					} catch (FileNotFoundException e) {
    			    	System.out.println( getLog(Log.ERROR) + e.getMessage());
    				} catch (IOException e) {
    					System.out.println( getLog(Log.ERROR) + e.getMessage());
    				}
					
				}
    			
    			System.out.println( "                                                                          " );
    			System.out.println( getLog(Log.INFO) + "Processo concluido :)" );
    	
	}
	
	
	/**
	 * Retorna a LOG HEADER com a datatime do evento
	 * @param log o tipo de log desejada  .:. log.DEBUG
	 * @return String log .:. INFO LOG | 2014.01.01 09:06:53,950 | INFO LOG OHLOC | 
	 */
	private static String getLog(Log log){
		
		String logType = "";
        if(Log.DEBUG == log) {
        	logType = "DEBUG";
        }else if(Log.INFO == log) {
        	logType = "INFO";
        }else if(Log.ERROR == log) {
        	logType = "ERROR";
        }else if(Log.FATAL == log) {
        	logType = "FATAL";
        }
		
		return logType +" | "+ ft.format( new Date() ) +" | OHLOC | "; 
	}

}


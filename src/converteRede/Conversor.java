package converteRede;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JOptionPane;

public class Conversor {
	static boolean mandeiEmail=false;
	JavaMailApp appMail = new JavaMailApp();
	//caminho dos arquivos utilizados
	static String log = "";
	File arquivoRede;
	File pastaDestino;
	File pastaDestinoProcessados;
	Date d = new Date(System.currentTimeMillis());
	SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yyyy");
	DecimalFormat df = new DecimalFormat("#,##0.00");
	DecimalFormat df2 = new DecimalFormat("0,00");
	//Lista que guarda todos os cartoes passados, no caso de credito parcelado
	ArrayList<Parcela> lcto = new ArrayList<>();
	//variáveis auxiliares para geracao do arquivo de saida
	String saida = "";	
	String dataEmissao;
	String a;
	String tipoDeRegistro = "";
	String detalhamento = "";
	String bandeira;	
	String dataDeVcto = "";
	String dataDeVctoAnterior = "";
	String dataDeEmissao = "";
	String dataDaBaixa;
	String dataDaBaixaAnterior;
	String hora;
	String nrCartao;
	String valorTitulo="";
	String valorBaixa;
	String txAdm;
	String vlrBruto;
	String incremento;
	String codConta;
	String dataDoArquivo;
	String sufixoRVstr = "";
	
	static String tipoDeArquivo;
	
	String dado;
	long banco;
	long agencia;
	long conta;
	
	long nrRV=0;
	long nrRVanterior=0;
	long cv;
	BigDecimal big1;
	BigDecimal big2;
	BigDecimal big3;
	BigDecimal big4;
	BigDecimal big5;
	BigDecimal big6;
	//variaveis de controle
	String auxS;
	float auxF;
	boolean fimLcto;
	boolean ultimoLcto;
	
	int coluna=0;
	int posicaoInicial=0;
	int posicaoFinal=0;
	int nrParcela=0;
	int auxI;
	int sufixoRV=1;
	
	public String converter(File arquivoOrigem, File pastaDestino, File pastaDestinoProcessados){
		this.arquivoRede = arquivoOrigem;
		this.pastaDestino = pastaDestino;
		this.pastaDestinoProcessados = pastaDestinoProcessados;

        tipoDeArquivo = getTipoDeArquivo();
        if (tipoDeArquivo == null){
        	appMail.enviaEmail("Arquivo nao suportado", "Erro ConverteRede");
        	JOptionPane.showMessageDialog(null, "Arquivo não suportado", "Erro",0 );
        }
        else if (tipoDeArquivo.equals("EEVC")){
        	lctoCredito();
        }
        else if (tipoDeArquivo.equals("EEFI")){
        	baixaCredito();
        }
        else if (tipoDeArquivo.equals("EEVD")){
        	vendasDebito();
        }
        gerarArquivo(tipoDeArquivo);
        return dataDoArquivo;
	}
	//Arquivo EEVC
	public void lctoCredito(){
		try{
    		FileReader file = new FileReader(arquivoRede);
    		BufferedReader file2 = new BufferedReader(file);

    		a = file2.readLine();
    		fimLcto = false;

    		while (a!=null){
    			
    			tipoDeRegistro = a.substring(0, 3);
    			//quando a venda é credito a vista
    			if (tipoDeRegistro.equals("002")){
    				dataDoArquivo = a.substring(3,5)+"-"+a.substring(5, 7)+"-"+a.substring(7,11);
    			}
    			
    			if (tipoDeRegistro.equals("006")){
    				if (fimLcto){
    					dataDeVctoAnterior=dataDeVcto;
    					nrRVanterior = nrRV;
    					escreveLctoCredito();
    				}
    				nrParcela = 0;
    				dataDeVcto = a.substring(128,136);
    				dataDeEmissao = a.substring(40,48);
    				nrRV = Long.parseLong(a.substring(12,21));
    				//quando tem o mesmo numero de RV
    				if ((nrRV == nrRVanterior) ) {
    					sufixoRV++;
    					sufixoRVstr = "-"+String.valueOf(sufixoRV);
    				}
    				else {
    					sufixoRV=1;
    					sufixoRVstr = "";
    				}
    				
    				valorTitulo = String.valueOf(Long.parseLong(a.substring(53,68)));
    				valorTitulo = valorTitulo.substring(0, valorTitulo.length()-2)+","+valorTitulo.substring(valorTitulo.length()-2, valorTitulo.length());
    				txAdm = String.valueOf(Long.parseLong(a.substring(98,113)));
    				if (txAdm.length()<3){
    					txAdm = "0"+txAdm;
    				}
    				txAdm = txAdm.substring(0, txAdm.length()-2)+","+txAdm.substring(txAdm.length()-2, txAdm.length());
    				bandeira = getFormaDePgtoCredito(a.substring(136,137));
    				nrParcela = 1;
    			}else if(tipoDeRegistro.equals("008")){
    				fimLcto=true;
    				cv = Long.parseLong(a.substring(86,98));
    				vlrBruto = String.valueOf(Long.parseLong(a.substring(37,52)));
    				vlrBruto = vlrBruto.substring(0, vlrBruto.length()-2)+","+vlrBruto.substring(vlrBruto.length()-2, vlrBruto.length());
    				hora = a.substring(132,134)+":"+a.substring(134,136);
    				nrCartao = a.substring(67,83);
    				detalhamento = detalhamento + "CV:"+cv+" / Valor Parcela:"+vlrBruto+" / Valor Total:"+vlrBruto+"|";
    			}
    			//quando é um crédito parcelado
    			else if (tipoDeRegistro.equals("010")){
    				if (fimLcto){
    					dataDeVctoAnterior=dataDeVcto;
    					nrRVanterior = nrRV;
    					escreveLctoCredito();
    				}
    				nrParcela = 0;
    				bandeira = getFormaDePgtoCredito(a.substring(136,137));
    			}
    			else if (tipoDeRegistro.equals("012")){
    				Parcela auxParcela = new Parcela();
    				auxParcela.hora = a.substring(134,136)+":"+a.substring(136,138);
    				auxParcela.nrCartao = a.substring(67,83);
    				auxParcela.nrCV = Long.parseLong(a.substring(88,100));
    				auxParcela.nrRV = Long.parseLong(a.substring(12,21));
    				auxParcela.setQtdeParcelas(Integer.parseInt(a.substring(86,88)));
    				auxS = String.valueOf(Long.parseLong(a.substring(220,235)));
    				auxParcela.valor1Parcela =  auxS.substring(0, auxS.length()-2)+","+auxS.substring(auxS.length()-2, auxS.length());
    				auxS = String.valueOf(Long.parseLong(a.substring(235,250)));
    				auxParcela.valorDemaisParcelas = auxS.substring(0, auxS.length()-2)+","+auxS.substring(auxS.length()-2, auxS.length());
    				auxS = String.valueOf(Long.parseLong(a.substring(37,52)));
    				auxS = auxS.substring(0, auxS.length()-2)+"."+auxS.substring(auxS.length()-2, auxS.length());
    				auxParcela.valorTotal = auxS.replace('.', ',');
    				big1 = new BigDecimal(auxS);
    				big2 = new BigDecimal(auxParcela.qtdeParcelas);
    				big3 = big1.divide(big2,2,RoundingMode.HALF_UP);
    				auxParcela.valorBrutoParcelas = big3.toString().replace('.', ',');
    				big4 = big3.multiply(big2);
    				big5 = big1.subtract(big4);
    				big6 = big3.add(big5);
    				auxParcela.valorBrutoUltimaParcela = big6.toString().replace('.', ',');
    				
    				lcto.add(auxParcela);
    			}
    			else if (tipoDeRegistro.equals("014")){
    				nrParcela++;
    				dataDeVcto = a.substring(84,92);
    				dataDeEmissao = a.substring(21,29);
    				nrRV = Long.parseLong(a.substring(12,21));
    				if ((nrRV == nrRVanterior) ) {
    					sufixoRV++;
    					sufixoRVstr = "-"+String.valueOf(sufixoRV);
    				}
    				else {
    					sufixoRV=1;
    					sufixoRVstr = "";
    				}
    				valorTitulo = String.valueOf(Long.parseLong(a.substring(39,54)));
    				valorTitulo = valorTitulo.substring(0, valorTitulo.length()-2)+","+valorTitulo.substring(valorTitulo.length()-2, valorTitulo.length());
    				txAdm = String.valueOf(Long.parseLong(a.substring(54,69)));
    				if (txAdm.length()<3){
    					txAdm = "0"+txAdm;
    				}
    				txAdm = txAdm.substring(0, txAdm.length()-2)+","+txAdm.substring(txAdm.length()-2, txAdm.length());
    				for (int i=0;i<lcto.size();i++){
    					Parcela auxParcela = new Parcela();
    					auxParcela = lcto.get(i);
    					incremento = auxParcela.qtdeParcelas-(auxParcela.qtdeRestante-1)+"/"+auxParcela.qtdeParcelas;

    					if(lcto.get(i).qtdeRestante!=1){        						
    						detalhamento = detalhamento+"CV:"+auxParcela.nrCV+"-"+incremento+ " / Valor Parcela:"+auxParcela.valorBrutoParcelas+" / Valor Total:"+auxParcela.valorTotal+"|";
    					}
    					else{
    						detalhamento = detalhamento+"CV:"+auxParcela.nrCV+"-"+incremento+ " / Valor Parcela:"+auxParcela.valorBrutoUltimaParcela+" / Valor Total:"+auxParcela.valorTotal+"|";
    					}
    					
    					lcto.get(i).qtdeRestante--;
    					if (lcto.get(i).qtdeRestante==0){
    						lcto.remove(i);
    						i--;
    					}
    				}
    				dataDeVctoAnterior=dataDeVcto;
    				nrRVanterior = nrRV;
    				escreveLctoCredito();
    				
    			}
    			else{
    				if (fimLcto){
    					dataDeVctoAnterior=dataDeVcto;
    					nrRVanterior = nrRV;
    					escreveLctoCredito();
    				}
    			}
    			a = file2.readLine();
    			
    		}
    		file2.close();
         }catch(IOException ex){
        	 appMail.enviaEmail("Erro ao fazer leitura do arquivo de Cartão de Credito: \n"+ex, "Erro ConverteRede");
        	 JOptionPane.showMessageDialog(null, ex);
        	 log = log+ex+"\n";
         }
	}
	//Arquivo EEFI
	public void baixaCredito(){
		try{
    		FileReader file = new FileReader(arquivoRede);
    		BufferedReader file2 = new BufferedReader(file);
    		a = file2.readLine();
    		
    		while (a!=null){
    			tipoDeRegistro = a.substring(0, 3);
    			if (tipoDeRegistro.equals("030")){
    				dataDoArquivo = a.substring(3,5)+"-"+a.substring(5, 7)+"-"+a.substring(7, 11);
    			}
    			if (tipoDeRegistro.equals("034")){
    				InterfaceBanco interfaceBanco = new InterfaceBanco();
    				nrRV = Long.parseLong(a.substring(75,84));
    				if ((nrRV == nrRVanterior)) {
    					sufixoRV++;
    					sufixoRVstr = "-"+String.valueOf(sufixoRV);
    				}
    				else {
    					sufixoRV=1;
    					sufixoRVstr = "";
    				}
    				banco = Long.parseLong(a.substring(47, 50));
    				agencia = Long.parseLong(a.substring(50,56));
    				conta = Long.parseLong(a.substring(56, 67));
    				codConta = getContaBanco(banco, agencia, conta);
    				bandeira = getFormaDePgtoCredito(a.substring(92, 93));
    				dataDeVcto = dataDaBaixa = a.substring(23, 31);
    				//Valor Lançado em conta corrente
    				auxS = String.valueOf(Long.parseLong(a.substring(31, 46)));
    				if (!auxS.equals("0")) {
    					auxS = auxS.substring(0, auxS.length()-2)+"."+auxS.substring(auxS.length()-2, auxS.length());
    				}    				
    				big1 = new BigDecimal(auxS);
    				//nr da Parcela
    				auxS = a.substring(124, 126);
    				nrParcela = Integer.parseInt(auxS);
    				//Nr do RV
    				auxS = String.valueOf(nrRV);
    				auxI = Integer.parseInt(auxS);
    				//Tx Adm
    				txAdm = interfaceBanco.obterDesconto(auxI, nrParcela);
    				if (txAdm!=null){
    					System.out.println(txAdm);
        				big2 = new BigDecimal(txAdm.replace(',', '.'));
        				big3 = big1.add(big2);
        				valorBaixa = big3.toString().replace('.', ',');
        				dataDeVctoAnterior=dataDeVcto;
        				nrRVanterior = nrRV;
        				escreveBaixaCredito();
    				}
    				interfaceBanco.finalizarConexao();
    				
    			}
    			//antecipações
    			if (tipoDeRegistro.equals("036")){
    				InterfaceBanco interfaceBanco = new InterfaceBanco();
    				nrRV = Long.parseLong(a.substring(67, 76));
    				banco = Long.parseLong(a.substring(47, 50));
    				agencia = Long.parseLong(a.substring(50, 56));
    				conta = Long.parseLong(a.substring(56, 67));
    				codConta = getContaBanco(banco, agencia, conta);
    				bandeira = getFormaDePgtoCredito(a.substring(151, 152));
    				//Nr da Parcela
    				auxS = a.substring(107, 109);
    				nrParcela = Integer.parseInt(auxS);
    				//data de vencimento original
    				dataDeVcto = a.substring(99, 107);
    				//data de credito em conta correte
    				dataDaBaixa = a.substring(23, 31);
    				if ((nrRV == nrRVanterior) && dataDeVcto.equals(dataDaBaixa)) {
    					sufixoRV++;
    					sufixoRVstr = "-"+String.valueOf(sufixoRV);
    				}
    				else {
    					sufixoRV=1;
    					sufixoRVstr = "";
    				}
    				//valor do crédito original
    				auxS = String.valueOf(Long.parseLong(a.substring(84, 99)));
    				auxS = auxS.substring(0, auxS.length()-2)+"."+auxS.substring(auxS.length()-2, auxS.length());
    				big1 = new BigDecimal(auxS);
    				//valor lancado em conta corrente
    				auxS = String.valueOf(Long.parseLong(a.substring(31, 46)));
    				auxS = auxS.substring(0, auxS.length()-2)+"."+auxS.substring(auxS.length()-2, auxS.length());
    				big2 = new BigDecimal(auxS);
    				//desconto original
    				auxS = String.valueOf(nrRV);
    				auxI = Integer.parseInt(auxS);
    				auxS = interfaceBanco.obterDesconto(auxI, nrParcela);
    				if (auxS != null){
    					big3 = new BigDecimal(auxS.replace(',', '.'));
    					big4 = big1.add(big3);
    					valorBaixa = big4.toString().replace('.', ',');
    					big5 = big1.subtract(big2);
    					big6 = big5.add(big3);
    					txAdm = big6.toString().replace('.', ',');
    					dataDeVctoAnterior=dataDeVcto;
    					nrRVanterior = nrRV;
    					escreveBaixaCredito();
    				}
    				interfaceBanco.finalizarConexao();
    				
    			}
    			//ajustes
    			if(tipoDeRegistro.equals("035")){
    				nrRV = Long.parseLong(a.substring(12, 21));
    				auxS = String.valueOf(nrRV);
    				auxI = saida.indexOf(auxS);
    				if (auxI != -1){ //se encontrado  RV 
    					//obtendo o valor do ajuste de debito
    					auxS = String.valueOf(Long.parseLong(a.substring(29, 44)));
    					auxS = auxS.substring(0, auxS.length()-2)+"."+auxS.substring(auxS.length()-2, auxS.length());
    					if (auxS.length()<3){
        					auxS = "0"+auxS;
        				}
    					//valor do ajuste
    					big1 = new BigDecimal(auxS); //big1 é o valor do ajuste
    					//valor do crédito original
    					auxS = String.valueOf(Long.parseLong(a.substring(193,208))); 
    					if(nrRVanterior==nrRV){
    						auxS = String.valueOf(Long.parseLong(a.substring(178,193))); //quando tiver mais de 1 RV do mesmo ajuste,pegar esse valor.
    					}
    					auxS = auxS.substring(0, auxS.length()-2)+"."+auxS.substring(auxS.length()-2, auxS.length());
    					big2 = new BigDecimal(auxS);
    					//obtendo o valor da taxa administrativa
    					auxI = saida.indexOf(String.valueOf(nrRV));
    					if (auxI != -1){
    						int aux=0;
        					while (aux != 7){
        						auxI = saida.indexOf(';',auxI)+1;
        						aux++;
        					}
        					String auxTaxa = saida.substring(auxI,saida.indexOf(';',auxI));
        					System.out.println("Taxa:"+auxTaxa);
        					big3 = new BigDecimal(auxTaxa.replace(',', '.'));
        					//novo valor de baixa
        					big4 = big2.add(big3); // Valor do Credito Original + taxa ADM 
        					//inserindo o valor novo valor de baixa na linha correspondente
        					auxI = saida.indexOf(String.valueOf(nrRV));
        					if (auxI != -1){
        						int inicioValorAsubstituir=0;
        						int fimValorAsubstituir=0;
        						int aux2=0;
            					while (aux2 != 6){
            						auxI = saida.indexOf(';',auxI)+1;
            						aux2++;
            					}
            					inicioValorAsubstituir = auxI;
            					fimValorAsubstituir = saida.indexOf(';',auxI);
            					saida = saida.substring(0, inicioValorAsubstituir)+ big4.toString().replace('.', ',')+ saida.substring(fimValorAsubstituir);
            					
        					}
        					//inserindo o valor de ajuste na linha correspondente
        					auxI = saida.indexOf("BXCRE01",auxI);
        					auxI = auxI+7;
        					int fimValorTaxa = saida.indexOf('B',auxI);
        					if(fimValorTaxa < 0) {
        						fimValorTaxa = saida.length();
        					}
        					String taxaAnterior = saida.substring(auxI+1,fimValorTaxa-1);
        					BigDecimal valorTaxaAnterior = new BigDecimal(taxaAnterior.replace(',', '.'));
        					big1 = big1.add(valorTaxaAnterior);
        					auxS = big1.toString();
        					saida = saida.substring(0, auxI) + ";"+auxS.replace('.', ',')+ saida.substring(fimValorTaxa-1);
        					//atualizando o valor da baixa quando tiver mais de 1 ajuste pro mesmo RV
        					if(nrRVanterior==nrRV){
        						auxI = saida.indexOf(String.valueOf(nrRV));
            					if (auxI != -1){
            						int inicioValorAsubstituir=0;
            						int fimValorAsubstituir=0;
            						int aux2=0;
                					while (aux2 != 6){
                						auxI = saida.indexOf(';',auxI)+1;
                						aux2++;
                					}
                					inicioValorAsubstituir = auxI;
                					fimValorAsubstituir = saida.indexOf(';',auxI);
                					//novoValorDeBaixa = valor do Ajuste + valor da taxa + valor da baixa
                					big4 = big4.add(big1);
                					saida = saida.substring(0, inicioValorAsubstituir)+ big4.toString().replace('.', ',')+ saida.substring(fimValorAsubstituir);
                					
            					}
        					}
    					}
    					dataDeVctoAnterior=dataDeVcto;
    					nrRVanterior=nrRV;
    				}
    				
    				
    			}
    			a = file2.readLine();
    		}
    		file2.close();
    	}catch(IOException ioe){
    		appMail.enviaEmail("Erro ao fazer leitura do arquivo de Baixa dos Cartoes de Credito: \n"+ioe, "Erro ConverteRede");
    		JOptionPane.showMessageDialog(null, ioe);
    		log = log+ioe+"\n";
    	}
	}
	//Arquivo EEVD
	public void vendasDebito(){
		ArrayList<AjustesDebito> ajustesDebito = new ArrayList<>();
		try{
    		FileReader file = new FileReader(arquivoRede);
    		BufferedReader file2 = new BufferedReader(file);
    		a = file2.readLine();
    		fimLcto = false;
    		while (a!=null){
    			tipoDeRegistro = dadoDaColuna(1,a);
    			if (tipoDeRegistro.equals("00")){
    				String aux = dadoDaColuna(3, a);
    				dataDoArquivo = aux.substring(0, 2)+"-"+aux.substring(2, 4)+"-"+aux.substring(4, 8);
    			}
    			if (tipoDeRegistro.equals("01")){
    				if (fimLcto){
    					escreveLctoDebito();
    				}
    				
    				dataDaBaixa = dataDeVcto = dadoDaColuna(3, a);
    				dataDeEmissao = dadoDaColuna(4, a);
    				nrRV = Long.parseLong(dadoDaColuna(5, a));
    				auxS = String.valueOf(Long.parseLong(dadoDaColuna(7, a)));
    				valorTitulo = auxS.substring(0, auxS.length()-2)+","+auxS.substring(auxS.length()-2, auxS.length());
    				auxS = String.valueOf(Long.parseLong(dadoDaColuna(8, a)));
    				if (auxS.length()<3){
    					auxS = "0"+auxS;
    				}
    				txAdm = auxS.substring(0, auxS.length()-2)+","+auxS.substring(auxS.length()-2, auxS.length());
    				auxS = String.valueOf(Long.parseLong(dadoDaColuna(9, a)));
    				valorBaixa = auxS.substring(0, auxS.length()-2)+","+auxS.substring(auxS.length()-2, auxS.length());
    				banco = Long.parseLong(dadoDaColuna(11, a));
    				agencia = Long.parseLong(dadoDaColuna(12, a));
    				conta = Long.parseLong(dadoDaColuna(13, a));
    				codConta = getContaBanco(banco, agencia, conta);
    				auxS = dadoDaColuna(14, a);
    				bandeira = getFormaDePgtoDebito(auxS);
    			}
    			else if (tipoDeRegistro.equals("05")){
    				fimLcto = true;
    				auxS = String.valueOf(Long.parseLong(dadoDaColuna(5, a)));
    				vlrBruto = auxS.substring(0, auxS.length()-2)+","+auxS.substring(auxS.length()-2, auxS.length());
    				nrCartao = dadoDaColuna(7, a);
    				cv = Long.parseLong(dadoDaColuna(10, a));
    				detalhamento = detalhamento + "CV:"+cv+" / Valor Parcela:"+vlrBruto+" / Valor Total:"+vlrBruto+"|";
    			}else if(tipoDeRegistro.equals("011")) {
    				nrRV = Long.parseLong(dadoDaColuna(3,a));
    				auxS = String.valueOf(Long.parseLong(dadoDaColuna(5,a)));
    				auxS = auxS.substring(0, auxS.length()-2)+","+auxS.substring(auxS.length()-2, auxS.length());
    				if (auxS.length()<3){
    					auxS = "0"+auxS;
    				}
    				String valorAjusteDebito =  auxS;
    				AjustesDebito auxAjusteDeb = new AjustesDebito();
    				auxAjusteDeb.nrRV = nrRV;
    				auxAjusteDeb.valorAjusteDebito = valorAjusteDebito;
    				ajustesDebito.add(auxAjusteDeb);
    				
    			}
    			else{
    				if (fimLcto){
    					escreveLctoDebito();
    				}
    			}
    			posicaoInicial=0;
    			posicaoFinal=0;
    			coluna=0;
    			a=file2.readLine();

    		}
		
    		//procura os RVs da lista, e insere os Ajustes na linha correspondente
    		for (int i=0;i<ajustesDebito.size();i++) {
    			auxI = saida.indexOf(String.valueOf(ajustesDebito.get(i).nrRV));    			
    			if (auxI != -1){
    				auxI = saida.indexOf("BXCRE01",auxI);
    				auxI = auxI+7;//(inicio do valor taxa)
    				int fimValorTaxa = saida.indexOf('T',auxI);
    				if(fimValorTaxa < 0) {
    					fimValorTaxa = saida.length();
    				}
    				String taxaAnterior = saida.substring(auxI+1,fimValorTaxa-1);
    				BigDecimal valorTaxaAnterior = new BigDecimal(taxaAnterior.replace(',', '.'));
    				big1 = new BigDecimal(ajustesDebito.get(i).valorAjusteDebito.replace(',', '.'));
    				big1 = big1.add(valorTaxaAnterior);
    				auxS = big1.toString();
    				saida = saida.substring(0, auxI) + ";"+auxS.replace('.', ',')+ saida.substring(fimValorTaxa-1);
					
    			}
    			
    		
    		}

    		file2.close();
    	}catch(IOException e){
    		appMail.enviaEmail("Erro ao fazer leitura do arquivo de cartao de debito: \n"+e, "Erro ConverteRede");
    		JOptionPane.showMessageDialog(null, e);
    		log = log + e+"\n";
    	}
	}
	//Metodo para gerar o arquivo de saida para importar no radar
 	public void gerarArquivo(String tipoDeArquivo){
		try{
        	FileWriter fwriter = new FileWriter(pastaDestino+"/Rede"+tipoDeArquivo+"_"+dataDoArquivo+".txt");
        	PrintWriter pwriter = new PrintWriter(fwriter);
        	log = log+"Arquivo Gerado em: "+pastaDestino+"/Rede"+tipoDeArquivo+"_"+dataDoArquivo+".txt \n";
        	pwriter.print(saida);
        	fwriter.close();   	
        	saida = "";
        	moverArquivoProcessado(dataDoArquivo,tipoDeArquivo);
        }catch(IOException ex){
        	appMail.enviaEmail("Erro ao gerar arquivo de saida. \n Tipo de arquivo: "+tipoDeArquivo+" \n"+ex, "Erro ConverteRede");
        	JOptionPane.showMessageDialog(null, ex);
        	log = log + ex+"\n";
        }
	}
 	//Metodo para mover o arquivo da Rede já processado
 	public void moverArquivoProcessado(String data,String tipoDeArquivo){ 		
 		File pastaComData = new File(pastaDestinoProcessados+"/"+data);
 		File arquivoDestino = new File(pastaComData+"/"+arquivoRede.getName());
 		boolean deletado;
 		if(!pastaComData.exists()){
 			pastaComData.mkdir();
 		}
    	try{
    		if (arquivoDestino.exists()){
    			arquivoDestino.delete();
    		}
    		//copia arquivo para nova pasta de "processados"
    		FileInputStream fis = new FileInputStream(arquivoRede);
    		FileOutputStream fos = new FileOutputStream(arquivoDestino);
    		FileChannel sourceChannel = fis.getChannel();
    		FileChannel destinationChannel = fos.getChannel();
    		sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
    		//escreve arquivo de log
    		FileWriter fileLog = new FileWriter(pastaDestinoProcessados+"/"+data+"/Log"+tipoDeArquivo+"_"+data+".txt");
 			PrintWriter pwriter = new PrintWriter(fileLog);
 			pwriter.print(log);
 			fileLog.close();
 			sourceChannel.close();
 			destinationChannel.close();
 			fis.close();
 			fos.close();
 			//apaga arquivo processado
 			deletado = arquivoRede.delete();
 			log = "";
 			if (!deletado){
 				appMail.enviaEmail("Problema ao mover arquivo processado: Não foi possível apagar o arquivo de origem", "Erro ConverteRede");
 				JOptionPane.showMessageDialog(null, "Arquivo "+ arquivoRede.getAbsolutePath().toString()+" nao deletado");
 			}
 			
 		}catch(IOException e){
 			appMail.enviaEmail("Problema ao mover arquivo processado: \n"+e, "Erro ConverteRede");
 			JOptionPane.showMessageDialog(null, e);
    		log = log + e+"\n";
    	}
    }
 	//Com base no Nome do arquivo, obter o tipo de arquivo
    public String getTipoDeArquivo(){
    	String s = null;
    	switch(arquivoRede.getName().substring(0,3)) {
		case "DEB":
			s="EEVD";
			break;
		case "CRE":
			s="EEVC";
			break;
		case "FIN":
			s="EEFI";
			break;
		default:
			break;
    	}
    	return s;
    }
    //Escrever dados do arquivo de saída EEVC
    public void escreveLctoCredito(){
    	SimpleDateFormat conversorDeData = new SimpleDateFormat("dd-MM-yyyy");
    	java.sql.Date dataInclusao = null;
    	try{
    		dataInclusao = new java.sql.Date(conversorDeData.parse(dataDoArquivo).getTime());
    	}
    	catch (ParseException e){
    		appMail.enviaEmail("Erro ao converter Data, na hora de gerar e gravar no BD os dados do cartao de credito: +\n"+e, "Erro ConverteRede");
    		JOptionPane.showMessageDialog(null, "erro ao converter data: "+e);
    	}
    	String nrRvS = String.valueOf(nrRV);
    	InterfaceBanco interfaceBanco = new InterfaceBanco();
    	saida = saida + "T1;"+dataDeVcto+";34105;"+dataDeEmissao+";"+nrRV+sufixoRVstr+";002;"+valorTitulo+";01.02;\"ADM CARTAO\";NAOCONT;2683;1;"+bandeira+";"+detalhamento+"\n";
    	interfaceBanco.gravarDesconto(Integer.parseInt(nrRvS), nrParcela, txAdm, dataInclusao);
    	interfaceBanco.finalizarConexao();
    	dataDeVcto = "";
    	dataDeEmissao = "";
    	nrRV=0;
    	valorTitulo="";
    	detalhamento="";
    	
    	fimLcto=false;
    }
    //Escrever dados do arquivo de saída EEVD
    public void escreveLctoDebito(){
    	saida = saida + "T1;"+dataDeVcto+";34105;"+dataDeEmissao+";"+nrRV+";002;"+valorTitulo+";01.02;\"ADM CARTAO\";NAOCONT;2683;1;"+bandeira+";"+detalhamento+"\n";
    	saida = saida + "B"+codConta+";1;"+bandeira+";"+dataDaBaixa+";"+valorTitulo+";"+txAdm+";BXCRE01;0,00"+"\n";
    	
    	dataDeVcto = "";
    	dataDeEmissao = "";
    	nrRV = 0;
    	valorTitulo = "";
    	bandeira = "";
    	detalhamento = "";
    	codConta = "";
    	dataDaBaixa = "";
    	valorBaixa = "";
    	txAdm = "";
    	fimLcto = false;
    }
    //Escrever dados do arquivo de saída EEFI
    public void escreveBaixaCredito(){
    	saida = saida + "B34105;"+nrRV+sufixoRVstr+";1;"+codConta+";"+bandeira+";"+dataDeVcto+";"+dataDaBaixa+";"+valorBaixa+";"+txAdm+";BXCRE01;0,00"+"\n";
    	nrRV = 0;
    	codConta = "";
    	bandeira = "";
    	dataDeVcto = "";
    	dataDaBaixa = "";
    	valorBaixa = "";
    	txAdm = "";
    }
    //Obter o código da conta banco no radar com base dos dados de banco, agencia e conta do arquivo
    public String getContaBanco(long banco, long agencia, long conta){
    	String aux="";
    	
    	if (banco == 341){
    		if (agencia == 91){
    			if (conta == 651000){
    				aux = "140";
    			}
    			if (conta == 655605){
    				aux = "144";
    			}
    		}
    	}
    	else if (banco == 104){
    		if(agencia == 1979){
    			if (conta == 16153 || conta == 3000016153L){
    				aux = "98";
    			}
    			
    		}
    	}
    	return aux;
    }
    //Obter o código da forma de pagamento dos cartões de crédito no radar com base no código da bandeira existente no arquivo
    public String getFormaDePgtoCredito(String s){
    	String aux="";
    	if (s.equals("0")){ //outras bandeiras
    		aux = "";
    	}
    	else if (s.equals("1")){ //MasterCard
    		aux = "24";
    	}
    	else if (s.equals("2")){ //Diners Club
    		aux = "21";
    	}
    	else if (s.equals("3")){ //Visa
    		aux = "06";
    	}
    	else if (s.equals("4")){ //Cabal
    		aux = "";
    	}
    	else if (s.equals("5")){ //HiperCard
    		aux = "44";
    	}
    	else if (s.equals("6")){ //Sorocred
    		aux = "";
    	}
    	else if (s.equals("7")){ //CUP
    		aux = "";
    	}
    	else if (s.equals("8")){ //Credsystem(mais)
    		aux = "";
    	}
    	else if (s.equals("9")){ //Sicredi
    		aux = "";
    	}
    	else if (s.equals("A")){ //Hiper
    		aux = "";
    	}
    	else if (s.equals("X")){ //Amex
    		aux = "20";
    	}
    	else if (s.equals("E")){ //Elo
    		aux = "22";
    	}
    	return aux;
    }
    //Obter o código da forma de pagamento dos cartões de débito no radar com base no código da bandeira existente no arquivo
    public String getFormaDePgtoDebito(String s){
    	String aux="";
    	if (s.equals("0")){ //outras bandeiras
    		aux = "";
    	}
    	else if (s.equals("1")){ //MasterCard Maestro
    		aux = "25";
    	}
    	else if (s.equals("3")){ //Visa Débito
    		aux = "14";
    	}
    	else if (s.equals("4")){ //Cabal
    		aux = "";
    	}
    	else if (s.equals("9")){ //Sicredi
    		aux = "";
    	}
    	else if (s.equals("A")){ //Hiper
    		aux = "";
    	}
    	else if (s.equals("E")){ //Elo
    		aux = "22";
    	}
    	else if (s.equals("x")){ //Amex
    		aux = "20";
    	}
    	return aux;
    }
    //Ir para a próxima "coluna", depois da próxima vírgula (Método utilizado para o Extrato EEVD)
    public void proximaColuna(String a){
    	posicaoInicial = posicaoFinal+1;
    	posicaoFinal = a.indexOf(',',posicaoInicial);
    	if (posicaoFinal == -1){
    		posicaoFinal = a.length();
    	}
    	coluna++;
    }
    //Obter o dado da coluna desejada (Método utilizado para o Extrato EEVD)
    public String dadoDaColuna(int colunaDesejada, String a){
    	String aux="";
    	if(coluna < colunaDesejada){
    		while (coluna < colunaDesejada){
        		proximaColuna(a);
        	}
    		if (coluna > 1 ){
    			aux = a.substring(posicaoInicial,posicaoFinal);
    		}else{
    			aux = a.substring(posicaoInicial-1,posicaoFinal);
    		}	
    	}
    	return aux;
    }
    
    public static void escreveLog(String s){
    	log = log + s+"\n";
    }
    
    
}

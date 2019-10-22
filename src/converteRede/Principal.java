package converteRede;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

import javax.swing.JOptionPane;

public class Principal {

	public static void main(String[] args) {
		
		FileFilter filter = new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				// TODO Auto-generated method stub
				return pathname.getName().endsWith(".txt");
			}
		};
		File config = null; //arquivo de configuração
		File localDeOrigem = null; // pasta onde estao os arquivos de origem
		File[] arquivos = null; //arquivos de origem
		File destinoGerados= null; //pasta destino dos arquivos gerados
		File destinoProcessados = null; //pasta para onde vai os arquivos de origem processados
		String dataArquivo=null;
		
		Conversor conversor = new Conversor();
		JavaMailApp appMail = new JavaMailApp();
		
		FileReader file = null;
		BufferedReader file2 = null;
		String linha="";
		String caminho = System.getProperty("user.dir");
		String localDeOrigemS="";
		
		config = new File(caminho+"/config.txt");
		//leitura do arquivo de configuração
		try{
			file = new FileReader(config);
			file2 = new BufferedReader(file);
			//leitura do local de origem
			linha = file2.readLine();
			
		}catch(IOException ioe){
			appMail.enviaEmail("Erro ao fazer leitura do arquivo de configuração: \n"+ioe, "Teste Erro ConverteRede");
			JOptionPane.showMessageDialog(null, ioe);
		}
		localDeOrigemS = linha.substring(13);
		localDeOrigem = new File(localDeOrigemS);
		//leitura do local de destino
		try {
			linha = file2.readLine();
		} catch (IOException e) {
			appMail.enviaEmail("Erro ao fazer leitura do local de destino no arquivo de configuração: \n"+e, "Erro ConverteRede");
			JOptionPane.showMessageDialog(null, e);
		}
		destinoGerados = new File(linha.substring(21));
		//leitura do local dos arquivos processados
		try{
			linha = file2.readLine();
		}catch (IOException e){
			appMail.enviaEmail("Erro ao fazer leitura do local dos arquivos processados no arquivo de configuração: \n"+e, "Erro ConverteRede");
			JOptionPane.showMessageDialog(null, e);
		}
		destinoProcessados = new File(linha.substring(20));
		//processa uma lista de arquivos(todos os que estiverem dentro da pasta)
		InterfaceBanco interfacebanco;
		arquivos = localDeOrigem.listFiles(filter);
		for (int i=0;i<arquivos.length;i++){
			dataArquivo = conversor.converter(arquivos[i], destinoGerados,destinoProcessados);
			//insercao na tabela arquivos para ver depois se algum nao foi importado
			int codTipoArquivo=-1;
	        	        if (Conversor.tipoDeArquivo.equals("EEVC")){
	        	codTipoArquivo=0;
	        }
	        else if (Conversor.tipoDeArquivo.equals("EEFI")){
	        	codTipoArquivo=1;
	        }
	        else if (Conversor.tipoDeArquivo.equals("EEVD")){
	        	codTipoArquivo=2;
	        }
	        interfacebanco = new InterfaceBanco();	        
			interfacebanco.insereQtdeNoDiaAtual(dataArquivo,codTipoArquivo);
		}
		//exclui lancamentos antigos do banco de dados
		interfacebanco = new InterfaceBanco();	
		interfacebanco.excluirLctoAntigo();
		//inserir arquivos do dia atual
		if(arquivos.length>0) {
			
			
		}
		//verifica se houve importacao dia anterior, realiza as ações no banco e envia os email de alerta
		interfacebanco = new InterfaceBanco();
		interfacebanco.verificaArquivosDiasAnteriores();
	}
}

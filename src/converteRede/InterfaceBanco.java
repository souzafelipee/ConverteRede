package converteRede;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.swing.JOptionPane;


public class InterfaceBanco {
	JavaMailApp appMail = new JavaMailApp();
	public Connection con = null;
	public String url = "";
	public Statement stmt = null;
	private PreparedStatement pstmt = null;
	private PreparedStatement pstmt2 = null;
	public ResultSet res = null;
	private boolean mandeiEmail=false;
	
	public InterfaceBanco(){
		url = "jdbc:h2:file:C:/ConverteRede/PauliDatabase";
		try {
		    Class.forName("org.h2.Driver");
		}catch(ClassNotFoundException e){
			appMail.enviaEmail("Erro ao tentar se conectar ao Banco de dados: +\n Driver de conexao ao banco nao indentificado: \n"+e, "Erro ConverteRede");
			Conversor.escreveLog("Driver de conexão ao banco não identificado: "+e);
		}
		try{
			con = DriverManager.getConnection(url, "sa","");
			stmt = con.createStatement();
		}catch(SQLException e){
			appMail.enviaEmail("Nao foi possivel se conectar ao Banco de dados: \n"+e, "Erro ConverteRede");
			Conversor.escreveLog("Não foi possível se conectar ao banco de Dados: "+e);
		}
		//verifica se a tabela descontos existe, senão a cria:
		try{
			pstmt = con.prepareStatement("SHOW TABLES");
			res = pstmt.executeQuery();
			if (!res.next()){
				pstmt = con.prepareStatement("CREATE TABLE descontos(id INT IDENTITY(1,1) PRIMARY KEY,rv INT,parcela INT,valor VARCHAR(9) ,datainclusao DATE)");
				pstmt.execute();
			}
		}catch (SQLException e){
			appMail.enviaEmail("Erro de BD: Erro ao verificar se a tabela descontos existe: \n"+e, "Erro ConverteRede");
			JOptionPane.showMessageDialog(null, "Erro ao verificar se a tabela descontos existe: "+e);
			Conversor.escreveLog("Erro ao verificar se a tabela descontos existe: "+ e);
		}
	}
	
	public void gravarDesconto(int rv, int parcela,String valor,Date dataInclusao){
		Conversor.escreveLog("Entrou no método gravar desconto com RV="+rv+" e parcela="+parcela);
		String s = obterDesconto(rv, parcela);
		if (s==null){
			try {
				pstmt = con.prepareStatement("INSERT INTO descontos(rv,parcela,valor,datainclusao) VALUES (?,?,?,?)");
				pstmt.setInt(1, rv);
				pstmt.setInt(2, parcela);
				pstmt.setString(3, valor);
				pstmt.setDate(4, dataInclusao);
				int resultado = pstmt.executeUpdate();
				int resultadoShutdown = stmt.executeUpdate("shutdown");
				if (resultado >0){
					Conversor.escreveLog("Desconto e pra ter sido gravado. Resultado: "+resultado);
					Conversor.escreveLog("Resultado da execução do shutdown:"+resultadoShutdown);
				}
				else{
					Conversor.escreveLog("Desconto nao deve ter sido gravado. Resultado: "+resultado);
					Conversor.escreveLog("Resultado da execução do shutdown:"+resultadoShutdown);
				}
				
			} catch (SQLException e) {
				appMail.enviaEmail("Erro de BD: Erro ao gravar descontos na tabela: \n"+e, "Erro ConverteRede");
				Conversor.escreveLog("Erro ao gravar desconto: "+e);
			}
		}
		else{
			if(!Conversor.mandeiEmail) {
				appMail.enviaEmail("Problema de BD: \n Problema ao gravar descontos: \nProblema ao obter descontos: Possivelmente Desconto já está gravado no banco de dados", "Erro ConverteRede");
				Conversor.escreveLog("Desconto nao gravado");
				Conversor.mandeiEmail=true;
			}
			
		}
		
	}
	
	public String obterDesconto(int rv, int parcela){
		String s=null;
		try{
			res = stmt.executeQuery("SELECT valor FROM descontos WHERE rv="+rv+" AND parcela="+parcela);
			if (res.next()){
				s = res.getString("valor");
			}
		}catch(SQLException e){
			appMail.enviaEmail("Erro de BD: Erro ao obter descontos: \n"+e, "Erro ConverteRede");
			JOptionPane.showMessageDialog(null,"Erro ao obter desconto: "+ e);
			Conversor.escreveLog("Erro ao obter desconto: "+ e);
		}
		
		
		return s;
		
	}
	
	public void excluirLctoAntigo(){
		int s;
		try{
			res = stmt.executeQuery("SELECT id FROM descontos WHERE ((current_date - datainclusao)>365)");
			while (res.next()){
				s = res.getInt("id");
				res = stmt.executeQuery("DELETE FROM descontos WHERE id="+s);
			}
			stmt.execute("shutdown");
			
		}catch(SQLException e){
			appMail.enviaEmail("Erro de BD: Erro ao excluir lancamentos antigos: \n"+e, "Erro ConverteRede");
			JOptionPane.showMessageDialog(null,"Erro ao excluir lcto Antigo: "+ e);
			Conversor.escreveLog("Erro ao excluir lcto Antigo: "+e);
		}
	}
	
	public void finalizarConexao(){
		if (stmt != null){
			try{
				stmt.close();
			}
			catch(SQLException e){
				appMail.enviaEmail("Erro de BD: Erro ao finalizar conexao stmt: \n"+e, "Erro ConverteRede");
				JOptionPane.showMessageDialog(null, "Erro ao finalizar Conexao: " + e);
				Conversor.escreveLog("Erro ao finalizar Conexao: " + e);
			}
		}
		if (pstmt != null){
			try{
				pstmt.close();
			}
			catch(SQLException e){
				appMail.enviaEmail("Erro de BD: Erro ao finalizar conexao pstmt: \n"+e, "Erro ConverteRede");
				JOptionPane.showMessageDialog(null, "Erro ao finalizar Conexao: "+e);
				Conversor.escreveLog("Erro ao finalizar Conexao:" +e);
			}
		}
	}
	public void insereQtdeNoDiaAtual(String dataArquivo,int tipoDeArquivo){
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
		
		try {
			java.util.Date d = sdf.parse(dataArquivo);
			con = DriverManager.getConnection(url, "sa","");
			pstmt = con.prepareStatement("INSERT INTO arquivos (dataprocessamento,emailenviado,dataarquivo,tipoarquivo) VALUES (current_date(),false,?,?)");							
			pstmt.setDate(1, new java.sql.Date(d.getTime()));
			pstmt.setInt(2, tipoDeArquivo);
			pstmt.execute();
			
		}
		catch(SQLException e) {
			appMail.enviaEmail("Erro de BD: Erro ao inserir qtde de arquivos no dia atual: \n"+e, "Erro ConverteRede");
		}
		catch (ParseException e) {
			appMail.enviaEmail("Erro de BD: Erro ao Converter data:"+e, "Erro ConverteRede");
			e.printStackTrace();
		}
		
	}
	public int verificaArquivosDiasAnteriores(){
		int qtdeDiaAnterior=0;
		try {
			pstmt2 = con.prepareStatement("SELECT * FROM processado WHERE dataprocessamento=current_date()");
			res = pstmt2.executeQuery();
			boolean teveRegistro=false;
			boolean executado=false;
			while (res.next()){
				boolean resultado = res.getBoolean("processado");
				if(resultado) {
					executado=true;
				}
			}
			if (!executado) {
				pstmt2 = con.prepareStatement("INSERT INTO PROCESSADO(dataprocessamento,processado) VALUES (current_date(),TRUE)");				
				pstmt = con.prepareStatement("SELECT * FROM arquivos WHERE DATAARQUIVO=(current_date()-1)");
				pstmt2.executeUpdate();
				res = pstmt.executeQuery();			
				Date d;
				SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yyyy");
				String msg="";
				boolean eevc,eevd,eefi;
				eevc=eevd=eefi=false;
				while (res.next()){
					teveRegistro=true;
					qtdeDiaAnterior++;
					int tipoArquivo=res.getInt("tipoarquivo");
					switch (tipoArquivo){
						case 0:
							eevc=true;
							break;
						case 1:
							eevd=true;
							break;
						case 2:
							eefi=true;
							break;
					}				
				}
				//deletar da tabela arquivos o que está ok, 
				pstmt=con.prepareStatement("DELETE FROM arquivos WHERE dataarquivo=(current_date()-1) AND tipoarquivo=?");
				int tipoArquivo;
				if(teveRegistro) {
					if(eevc) {
						tipoArquivo=0;
						pstmt.setInt(1, tipoArquivo);
						pstmt.execute();
					}
					if(eevd){
						tipoArquivo=1;
						pstmt.setInt(1, tipoArquivo);
						pstmt.execute();				
					}
					if(eefi){
						tipoArquivo=2;
						pstmt.setInt(1, tipoArquivo);
						pstmt.execute();				
					}			
					//inserir na tabela pendentes o que não está ok
					pstmt=con.prepareStatement("INSERT INTO pendentes(dataprocessamento,emailenviado,tipoarquivo,dataarquivo,dataenvioemail) VALUES (null,false,?,current_date()-1,null)");
					if (!eevc) {
						tipoArquivo=0;
						pstmt.setInt(1, tipoArquivo);
						pstmt.execute();
					}
					if(!eevd) {
						tipoArquivo=1;
						pstmt.setInt(1, tipoArquivo);
						pstmt.execute();			
					}
					if (!eefi) {
						tipoArquivo=2;
						pstmt.setInt(1, tipoArquivo);
						pstmt.execute();
					}	
				}
				/*verificar arquivos que data de processamento é do dia anterior mas a data do arquivo nao e fazer 
				delete desses arquivos na tabela dos arquivos e dos pendentes */
				pstmt = con.prepareStatement("SELECT * FROM arquivos WHERE dataprocessamento=(current_date()-1) AND DATAARQUIVO<>(current_date()-1)");
				res = pstmt.executeQuery();
				pstmt = con.prepareStatement("DELETE FROM arquivos WHERE dataprocessamento=(current_date()-1) AND DATAARQUIVO<>(current_date()-1)");
				pstmt2 = con.prepareStatement("DELETE FROM pendentes WHERE dataarquivo=? AND tipoarquivo=?");
				while (res.next()) {
					//deletar da tabela pendentes
					appMail.enviaEmail("Arquivo \n Dia:"+res.getDate("dataarquivo")+"\n Tipo:"+res.getInt("tipoarquivo")+"\n que tinha sido perdido foi encontrado e importado!", "ConverteRede: Arquivo Encontrado");
					pstmt2.setDate(1, res.getDate("dataarquivo"));
					pstmt2.setInt(2, res.getInt("tipoarquivo"));
					pstmt2.execute();	
				}
				pstmt.execute();
				//enviar email de todos os arquivos que estão pendentes
				pstmt=con.prepareStatement("UPDATE pendentes SET dataenvioemail=null,emailenviado=false WHERE dataenvioemail<>current_date()");
				pstmt.execute();
				pstmt=con.prepareStatement("SELECT * FROM pendentes WHERE emailenviado<>true");
				res = pstmt.executeQuery();
				msg="Arquivos ainda não importados:";
				boolean enviaEmail=false;
				while(res.next()) {
					msg = msg+ "\nData:"+res.getDate("dataarquivo");
					msg = msg+ "\nTipoArquivo:"+res.getInt("tipoarquivo")+"\n \n";
					enviaEmail=true;
				}
				if(enviaEmail) {
					appMail.enviaEmail(msg, "ConverteRede: Arquivos nao importados");
				}
				pstmt = con.prepareStatement("UPDATE pendentes SET dataenvioemail=current_date(),emailenviado=true WHERE emailenviado<>true");
				pstmt.execute();			
				
			}
		}
		catch(SQLException e) {
			appMail.enviaEmail("Erro de BD: Erro ao obter data dos arquivos anteriores: \n"+e, "Erro ConverteRede");
		}
		return qtdeDiaAnterior;
		}
	
}
